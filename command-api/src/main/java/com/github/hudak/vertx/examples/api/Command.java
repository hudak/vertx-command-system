package com.github.hudak.vertx.examples.api;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;

import java.util.List;

/**
 * Created by hudak on 6/28/17.
 */
public interface Command {
    void run(List<String> arguments, Handler<AsyncResult<Void>> handler);

    static Command createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(Command.class, vertx, address);
    }
}
