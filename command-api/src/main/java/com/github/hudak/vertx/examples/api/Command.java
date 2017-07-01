package com.github.hudak.vertx.examples.api;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;

import java.util.List;

/**
 * Created by hudak on 6/28/17.
 */
@ProxyGen
@FunctionalInterface
public interface Command {
    void run(List<String> arguments, Handler<AsyncResult<String>> handler);

    @GenIgnore
    static Command createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(Command.class, vertx, address);
    }

    @GenIgnore
    static Disposable registerService(Vertx vertx, Command instance, String address) {
        MessageConsumer<JsonObject> consumer = ProxyHelper.registerService(Command.class, vertx, instance, address);
        return Disposables.fromAction(() -> ProxyHelper.unregisterService(consumer));
    }
}
