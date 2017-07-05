package com.github.hudak.vertx.examples;

import com.github.hudak.vertx.common.RxAdapter;
import com.github.hudak.vertx.examples.api.Command;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Created by hudak on 6/28/17.
 */
public class REPL extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(REPL.class);
    private Console console;
    private ServiceDiscovery serviceDiscovery;

    public static void main(String[] args) {
        VertxOptions options = new VertxOptions();
        options.setClusterManager(new HazelcastClusterManager());
        Vertx.clusteredVertx(options, REPL::startup);
    }

    private static void startup(AsyncResult<Vertx> asyncResult) {
        if (asyncResult.succeeded()) {
            Vertx vertx = asyncResult.result();
            vertx.deployVerticle(new REPL());
        } else {
            LOG.error("Start-up failed", asyncResult.cause());
            System.exit(1);
        }
    }

    @Override
    public void start() throws Exception {
        console = System.console();
        serviceDiscovery = ServiceDiscovery.create(vertx);

        // Start main loop
        mainLoop();
    }

    @Override
    public void stop() throws Exception {
        serviceDiscovery.close();
    }

    private void mainLoop() {
        // Read a line from console
        Maybe<String> line = Maybe.fromCallable(() -> console.readLine("cmd> ")).subscribeOn(Schedulers.io());

        // Split line into parts
        Observable<String> parts = line.map(Pattern.compile("\\s+")::split).flattenAsObservable(Arrays::asList).cache();

        // Locate command
        Single<Command> command = parts.firstElement() // Get first element
                .flatMapSingleElement(name -> name.isEmpty() ? Single.just(this::listCommands) : findCommand(name))
                .toSingle(this::shutdown); // Null input -> shutdown
        Single<List<String>> args = parts.skip(1).toList();

        // Locate and run command
        Single<Future<String>> futureResult = Single.zip(command, args, (cmd, arguments) -> {
            Future<String> future = Future.future();
            cmd.run(arguments, future);
            return future;
        });
        Maybe<String> result = futureResult.flatMapMaybe(RxAdapter::fromFuture);

        // Finish
        Completable done = result
                // Print output, if any
                .observeOn(Schedulers.io()).doOnSuccess(console.writer()::println)
                // Log any errors and ignore
                .doOnError(e -> LOG.error("error running command", e)).onErrorComplete()
                // Only interested in completion
                .ignoreElement();

        // repeat loop
        done.subscribe(() -> vertx.runOnContext(nothing -> mainLoop()));
    }

    private Single<Command> findCommand(String name) {
        JsonObject filter = new JsonObject()
                .put("type", EventBusService.TYPE)
                .put("service.interface", Command.class.getName())
                .put("name", name);

        Future<Record> record = Future.future();
        serviceDiscovery.getRecord(filter, record);

        return RxAdapter.fromFuture(record)
                .switchIfEmpty(Maybe.error(new NoSuchElementException("Command '" + name + "' not found")))
                .toSingle()
                .map(found -> found.getLocation().getString(Record.ENDPOINT))
                .map(endpoint -> Command.createProxy(vertx, endpoint));
    }

    private void listCommands(List<String> args, Handler<AsyncResult<String>> handler) {
        JsonObject filter = new JsonObject()
                .put("type", EventBusService.TYPE)
                .put("service.interface", Command.class.getName());

        Future<List<Record>> records = Future.future();
        serviceDiscovery.getRecords(filter, records);

        Observable<Record> recordObservable = RxAdapter.fromFuture(records).flattenAsObservable(list -> list);
        Single<List<String>> commands = recordObservable.map(Record::getName).toList();

        Single<String> result = commands.map(list -> new JsonObject().put("available_commands", list).encodePrettily());

        result.to(RxAdapter::future).setHandler(handler);
    }

    private void shutdown(List<String> args, Handler<AsyncResult<String>> handler) {
        LOG.info("done");
        vertx.close();
    }

}
