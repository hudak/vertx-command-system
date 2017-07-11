package com.github.hudak.vertx.common;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by hudak on 7/6/17.
 */
public class GuessPi implements Publisher<Boolean> {
    private static final io.vertx.core.logging.Logger LOG = LoggerFactory.getLogger(GuessPi.class);
    private final int sampleSize;
    private final Random random = new Random();

    public static void main(String[] args) {
        int numberOfGuesses = 2 << 6;
        Single.just(numberOfGuesses)
                .repeat(Runtime.getRuntime().availableProcessors())
                .flatMap(GuessPi::new)
                .observeOn(Schedulers.computation())
                .reduce(Sample.empty(), Sample::add)
                .flatMapMaybe(Sample::pi).map(String::valueOf)
                .doOnSuccess(System.out::println)
                // Wait for completion
                .ignoreElement().blockingAwait();
    }

    private GuessPi(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    @Override
    public void subscribe(Subscriber<? super Boolean> s) {
        Single.fromCallable(this::guess)
                .repeat(sampleSize)
                .subscribeOn(Schedulers.computation())
                .subscribe(s);
    }

    private boolean guess() {
        return random.doubles(2).map(d -> d * d).sum() <= 1;
    }

    private static class Sample {
        private final int count;
        private final double hits;

        Sample(int count, double hits) {
            this.count = count;
            this.hits = hits;
        }

        static Sample empty() {
            return new Sample(0, 0);
        }

        Sample add(boolean hit) {
            return new Sample(count + 1, hit ? hits + 1 : hits);
        }

        Maybe<Double> pi() {
            return count == 0 ? Maybe.empty() : Maybe.just(hits / count * 4);
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject()
                    .put("count", count)
                    .put("hits", hits);
            pi().subscribe(pi -> json.put("pi", pi));
            return json;
        }

        @Override
        public String toString() {
            return toJson().encodePrettily();
        }
    }
}
