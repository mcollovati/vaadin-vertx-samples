package com.github.mcollovati.vertx.vaadin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by marco on 16/07/16.
 */
public class FirstVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) throws Exception {
        vertx.createHttpServer()
            .requestHandler( req -> req.response().end("Mandi"))
        .listen(8080, result -> {
            if (result.succeeded()) {
                fut.complete();
            } else {
                fut.fail(result.cause());
            }
        });
    }
}
