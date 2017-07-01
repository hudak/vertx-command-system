package com.github.hudak.vertx.spark;

import com.github.hudak.vertx.common.RxAdapter;
import com.github.hudak.vertx.examples.api.Command;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

/**
 * Created by hudak on 6/30/17.
 */
public class SparkPi implements Command {
    private final JavaSparkContext context;

    protected SparkPi(SparkContext context) {
        this.context = JavaSparkContext.fromSparkContext(context);
    }

    public static void main(String[] args) throws Exception {
        SparkContext context = new SparkContext("local", "sparkPi");
        SparkPi sparkPi = new SparkPi(context);
        Future<String> estimate = Future.future();
        sparkPi.run(Arrays.asList(args), estimate);
        System.out.println("pi is " + RxAdapter.fromFuture(estimate).toSingle().blockingGet());
    }

    @Override
    public void run(List<String> arguments, Handler<AsyncResult<String>> handler) {
        Single<Integer> sampleSize = Observable.fromIterable(arguments).firstElement() // Get first argument
                .map(Integer::parseInt) // Parse as an integer
                .toSingle(10);

        // Run execution on a new thread
        Single<Double> estimate = sampleSize.observeOn(Schedulers.newThread()).map(this::estimatePi);

        // Write back string value
        estimate.map(String::valueOf).to(RxAdapter::future).setHandler(handler);
    }

    private double estimatePi(int sampleSize) {
        final long trials = 2L << sampleSize;
        long hits = context.parallelize(Collections.singletonList(trials))
                .flatMap(count -> LongStream.range(0, count).iterator())
                .repartition(context.defaultParallelism())
                .map(i -> new Tuple2<>(Math.random(), Math.random()))
                .keyBy(tuple -> tuple._1 * tuple._1 + tuple._2 * tuple._2)
                .filter(pair -> pair._1 < 1)
                .count();

        return 4.0 * hits / trials;
    }
}
