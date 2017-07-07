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
public class RxAdapter {
    public static <T> Maybe<T> fromFuture(Future<T> future) {
        MaybeSubject<AsyncResult<T>> subject = MaybeSubject.create();
        future.setHandler(subject::onSuccess);
        return subject.flatMap(async -> async.failed() ? Maybe.error(async.cause()) : Maybe.fromCallable(async::result));
    }

    public static <T> Maybe<T> fromFuture(Handler<Future<T>> handler) {
        return fromFuture(Future.future(handler));
    }

    public static <R> Future<R> future(Maybe<R> maybe) {
        Future<R> future = Future.future();
        maybe.subscribe(future::complete, future::fail, future::complete);
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

    private RxAdapter() {
    }
}
