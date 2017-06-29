package com.github.hudak.vertx.common;

import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * Created by hudak on 6/29/17.
 */
public class Launcher extends io.vertx.core.Launcher {
    public static void main(String[] args) {
        new Launcher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        options.setClustered(true);
        options.setClusterManager(new HazelcastClusterManager());
    }
}
