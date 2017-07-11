package com.github.hudak.vertx.common;

import com.google.common.truth.Truth;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by hudak on 7/10/17.
 */
public class FutureAsync {

    private String getThreadName() {
        return Thread.currentThread().getName();
    }

    @Test
    public void twoAsyncFutures() {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<String> first = CompletableFuture.supplyAsync(this::getThreadName, executor);
        CompletableFuture<String> second = CompletableFuture.supplyAsync(this::getThreadName, executor);

        Set<String> names = Stream.of(first, second).map(CompletableFuture::join).collect(Collectors.toSet());

        executor.shutdown();

        assertThat(names).hasSize(2);
    }

    @Test
    public void twoCompletableFutures() throws Exception {
        HashSet<String> names = new HashSet<>();

        Observable.range(0, 5)
                .flatMapSingle(i -> Single.fromCallable(this::getThreadName).subscribeOn(Schedulers.newThread()))
                .doOnNext(names::add)
                .ignoreElements().blockingAwait();

        assertThat(names).hasSize(5);
    }
}
