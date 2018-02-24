package com.github.mcollovati.vertx.vaadin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;
import io.vertx.core.Handler;

public class UIProxy {

    private final UI ui;
    private final VaadinSession session;
    private final VertxVaadinService service;

    public UIProxy(UI ui) {
        this.ui = ui;
        this.service = (VertxVaadinService) ui.getSession().getService();
        this.session = ui.getSession();
    }

    public Future<Void> runLater(Runnable runnable) {
        return runLater(runnable, 0, TimeUnit.MILLISECONDS);
    }

    public Future<Void> runLater(Runnable runnable, long delay, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Handler<Void> handler = makeHandler(future, runnable);
        if (delay <= 0) {
            service.getVertx().runOnContext(handler);
        } else {
            service.getVertx().setTimer(unit.toMillis(delay), id -> handler.handle(null));
        }
        return future;
    }


    private Handler<Void> makeHandler(CompletableFuture<Void> completer, Runnable runnable) {
        return ev -> service.runOnCurrentSession(this.session, freshSession -> {
            try {
                UI freshUI = freshSession.getUIById(ui.getUIId());
                if (freshUI != null) {
                    freshUI.access(runnable).get();
                    completer.complete(null);
                } else {
                    throw new UIDetachedException("Not found UI with id " + ui.getUIId());
                }
            } catch (Exception ex) {
                completer.completeExceptionally(ex);
            }
        });
    }

    public static <T> CompletableFuture<T> makeCompletableFuture(Future<T> future) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
