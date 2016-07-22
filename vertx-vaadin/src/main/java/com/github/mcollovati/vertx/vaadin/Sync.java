package com.github.mcollovati.vertx.vaadin;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Created by marco on 22/07/16.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sync {

    public static <T> T await(Consumer<Handler<AsyncResult<T>>> task) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Future<T> f = Future.<T>future().setHandler( ar -> {
                countDownLatch.countDown();
                if (ar.failed()) {
                    throw new VertxException(ar.cause());
                }
            });
            task.accept(f.completer());
            countDownLatch.await();
            return f.result();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VertxException(e);
        }

    }


}
