package com.github.hudak.vertx.spark;

import com.github.hudak.vertx.common.RxAdapter;
import com.github.hudak.vertx.examples.api.Command;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import org.apache.spark.SparkContext;

import java.util.UUID;

/**
 * Created by hudak on 6/30/17.
 */
public class Main extends AbstractVerticle {
    private ServiceDiscovery discovery;
    private Record publishRecord;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        discovery = ServiceDiscovery.create(vertx);
        Single<SparkContext> context = Single.fromCallable(SparkContext::getOrCreate).subscribeOn(Schedulers.computation());

        String address = "spark-" + UUID.randomUUID();

        // Create the sparkPi command
        Single<SparkPi> sparkPi = context.map(SparkPi::new);
        Record record = EventBusService.createRecord("sparkPi", address, Command.class);

        Single<Disposable> register = sparkPi.map(command -> Command.registerService(vertx, command, address));
        Maybe<Record> publish = Maybe.defer(() -> {
            Future<Record> future = Future.future();
            discovery.publish(record, future);
            return RxAdapter.fromFuture(future);
        });

        publish
                .doOnSuccess(this::setPublishRecord)
                .ignoreElement()
                .andThen(register.toCompletable())
                .to(RxAdapter::<Void>future).setHandler(startFuture);

    }

    public void setPublishRecord(Record publishRecord) {
        this.publishRecord = publishRecord;
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        discovery.unpublish(publishRecord.getRegistration(), stopFuture);
        discovery.close();
    }
}
