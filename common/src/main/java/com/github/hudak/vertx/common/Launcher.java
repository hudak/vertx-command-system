package com.github.hudak.vertx.common;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.*;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.Console;


/**
 * Created by hudak on 6/29/17.
 */
public class Launcher extends io.vertx.core.Launcher {

    private static final Flowable<String> consoleInput = Flowable
            .generate(System::console, Launcher::readFromConsole)
            .subscribeOn(Schedulers.io())
            .publish().autoConnect();

    private static void readFromConsole(Console console, Emitter<String> emitter) {
        String line = console.readLine();
        if (line != null) {
            emitter.onNext(line);
        } else {
            emitter.onComplete();
        }
    }

    public static void main(String[] args) {
        new Launcher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        options.setClustered(true);
        options.setClusterManager(new HazelcastClusterManager());
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        System.out.println("Type Ctrl-D to exit");
        Completable shutdown = Completable.defer(() -> {
            Future<Void> future = Future.future();
            vertx.close(future);
            return RxAdapter.fromFuture(future).ignoreElement();
        });
        consoleInput.ignoreElements().andThen(shutdown).subscribe();
    }
}
