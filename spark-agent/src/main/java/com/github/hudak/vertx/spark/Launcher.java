package com.github.hudak.vertx.spark;

import io.vertx.core.Vertx;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerApplicationEnd;

/**
 * Created by hudak on 6/30/17.
 */
public class Launcher extends com.github.hudak.vertx.common.Launcher {
    private final JavaSparkContext context;

    public Launcher(SparkContext context) {
        this.context = new JavaSparkContext(context);
    }

    public static void main(String[] args) {
        new Launcher(SparkContext.getOrCreate()).dispatch(args);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        context.sc().addSparkListener(new SparkListener() {
            @Override
            public void onApplicationEnd(SparkListenerApplicationEnd applicationEnd) {
                vertx.close();
            }
        });
    }
}
