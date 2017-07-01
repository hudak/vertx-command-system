package com.github.hudak.vertx.echo;

import com.github.hudak.vertx.examples.api.Command;
import io.reactivex.disposables.Disposables;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ProxyHelper;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.joining;

/**
 * Created by hudak on 6/29/17.
 */
public class EchoService extends AbstractVerticle implements Command {
    private static final Logger log = LoggerFactory.getLogger(EchoService.class);
    private ServiceDiscovery discovery;
    private final Future<Record> publish = Future.future();

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        discovery = ServiceDiscovery.create(vertx);
        String address = "echo-" + UUID.randomUUID();

        // Create a record for this service
        Record record = EventBusService.createRecord("echo", address, Command.class);

        // Register the command
        MessageConsumer<JsonObject> consumer = ProxyHelper.registerService(Command.class, vertx, this, address);
        Disposables.fromAction(() -> ProxyHelper.unregisterService(consumer));

        // Publish record
        discovery.publish(record, publish);

        // Notify started when published
        publish.<Void>mapEmpty().setHandler(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        // Don't forget to clean up
        Future<Void> unpublish = Future.future();
        publish.map(Record::getRegistration)
                .compose(registration -> discovery.unpublish(registration, unpublish), unpublish);

        // Destroys left-over bindings
        discovery.close();

        unpublish.setHandler(stopFuture);
    }

    @Override
    public void run(List<String> arguments, Handler<AsyncResult<String>> handler) {
        log.info("echoing: %s", arguments);
        String result = arguments.stream().collect(joining(" "));
        handler.handle(Future.succeededFuture(result));
    }
}
