package com.github.hudak.vertx.common;

import com.hazelcast.util.Preconditions;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.MaybeSubject;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Created by hudak on 7/6/17.
 */
public class ParallelTask implements Callable<String> {
    private final int i;

    private ParallelTask(int i) {
        this.i = i;
    }

    public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(10);

        Maybe<Integer> maybe = Maybe.create(e -> future.whenComplete((v, t) -> {
            Optional.ofNullable(t).ifPresent(e::onError);
            Optional.ofNullable(v).ifPresent(e::onSuccess);
            e.onComplete();
        }));

        maybe.flatMapObservable(i -> Observable.range(0, i))
                .map(ParallelTask::new)
                .flatMapSingle(task -> Single.fromCallable(task).subscribeOn(Schedulers.computation()))
                .blockingSubscribe(System.out::println);
    }

    @Override
    public String call() throws Exception {
        return Thread.currentThread().getName() + ": " + i;
    }
}
