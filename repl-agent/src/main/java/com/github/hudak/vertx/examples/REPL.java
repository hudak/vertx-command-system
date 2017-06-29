package com.github.hudak.vertx.examples;

import com.github.hudak.vertx.common.RxAdapter;
import com.github.hudak.vertx.examples.api.Command;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.Console;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by hudak on 6/28/17.
 */
public class REPL extends AbstractVerticle {
    private static Logger LOG = LoggerFactory.getLogger(REPL.class);
    private Console console;
    private ServiceDiscovery serviceDiscovery;

    public static void main(String[] args) {
        VertxOptions options = new VertxOptions();
        options.setClusterManager(new HazelcastClusterManager());
        Vertx.clusteredVertx(options, REPL::startup);
    }

    private static void startup(AsyncResult<Vertx> asyncResult) {
        if (asyncResult.succeeded()) {
            asyncResult.result().deployVerticle(new REPL());
        } else {
            LOG.error("Start-up failed", asyncResult.cause());
            System.exit(1);
        }
    }

    @Override
    public void start() throws Exception {
        // Read commands from System.in, publish to event bus
        console = System.console();
        serviceDiscovery = ServiceDiscovery.create(vertx);

        // Register output listener
        vertx.eventBus().consumer("print", this::print);
        // Start main loop
        mainLoop();
    }

    private void print(Message<Object> message) {
        vertx.executeBlocking(future -> {
            // On a worker thread
            console.printf("%s%n", Json.encodePrettily(message.body()));
            future.complete();
        }, async -> {
            // Back in the event loop
            if (async.succeeded()) {
                message.reply(null);
            } else {
                message.fail(1, async.cause().getMessage());
            }
        });
    }

    private void mainLoop() {
        vertx.executeBlocking(this::blockingRead, this::runCommand);
    }

    private void blockingRead(Future<String> future) {
        String readLine = console.readLine("cmd> ");
        future.complete(readLine);
    }

    private void runCommand(AsyncResult<String> asyncResult) {
        // Executes on an event loop thread
        if (asyncResult.failed()) {
            LOG.error("there was a problem", asyncResult.cause());
            vertx.close();
            return;
        }
        if (asyncResult.result() == null) {
            LOG.info("done");
            vertx.close();
            return;
        }

        Observable<String> line = Observable.fromArray(asyncResult.result().split("\\s+"));
        Maybe<String> command = line.firstElement().filter(s -> !s.isEmpty());
        List<String> args = line.skip(1).toList().blockingGet();

        command.flatMapSingleElement(this::findCommand)
                .defaultIfEmpty(this::listCommands)
                .flatMapCompletable(cmd -> runCommand(cmd, args))
                .onErrorResumeNext(e -> {
                    LOG.error("error running command", e);
                    return Completable.complete();
                })
                .doAfterTerminate(this::mainLoop)
                .subscribe();
    }

    private Single<Command> findCommand(String command) {
        JsonObject filter = new JsonObject()
                .put("type", EventBusService.TYPE)
                .put("service.interface", Command.class.getName())
                .put("name", command);

        Future<Record> record = Future.future();
        serviceDiscovery.getRecord(filter, record);

        return RxAdapter.fromFuture(record)
                .switchIfEmpty(Maybe.error(new NoSuchElementException("Command '" + command + "' not found")))
                .toSingle()
                .map(found -> found.getLocation().getString(Record.ENDPOINT))
                .map(endpoint -> Command.createProxy(vertx, endpoint));
    }

    private void listCommands(List<String> args, Handler<AsyncResult<Void>> handler) {
        JsonObject filter = new JsonObject()
                .put("type", EventBusService.TYPE)
                .put("service.interface", Command.class.getName());

        Future<List<Record>> records = Future.future();
        serviceDiscovery.getRecords(filter, records);

        Single<List<String>> commands = RxAdapter.fromFuture(records)
                .flattenAsObservable(list -> list)
                .map(Record::getName)
                .toList();

        Completable sent = commands.flatMapCompletable(list -> {
            JsonObject data = new JsonObject().put("available_commands", list);

            Future<Message<Void>> received = Future.future();
            vertx.eventBus().send("print", data, received);

            return RxAdapter.fromFuture(received).ignoreElement();
        });

        sent.to(RxAdapter::<Void>future).setHandler(handler);
    }

    private Completable runCommand(Command command, List<String> args) {
        Future<Void> done = Future.future();
        command.run(args, done);
        return RxAdapter.fromFuture(done).ignoreElement();
    }
}
