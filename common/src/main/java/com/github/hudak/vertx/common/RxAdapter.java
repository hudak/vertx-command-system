package com.github.hudak.vertx.common;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.MaybeSubject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * Created by hudak on 6/29/17.
 */
public class RxAdapter<T> implements Handler<AsyncResult<T>> {
    public static <T> Maybe<T> fromFuture(Future<T> records) {
        RxAdapter<T> adapter = new RxAdapter<>();
        records.setHandler(adapter);
        return adapter.subject;
    }

    public static <R> Future<R> future(Maybe<R> single) {
        Future<R> future = Future.future();
        single.subscribe(future::complete, future::fail, future::complete);
        return future;
    }

    public static <R> Future<R> future(Single<R> single) {
        Future<R> future = Future.future();
        single.subscribe(future::complete, future::fail);
        return future;
    }

    public static <R> Future<R> future(Completable completable) {
        Future<R> future = Future.future();
        completable.subscribe(future::complete, future::fail);
        return future;
    }

    private final MaybeSubject<T> subject = MaybeSubject.create();

    @Override
    public void handle(AsyncResult<T> event) {
        if (event.failed()) {
            subject.onError(event.cause());
        } else if (event.result() != null) {
            subject.onSuccess(event.result());
        } else {
            subject.onComplete();
        }
    }
}
