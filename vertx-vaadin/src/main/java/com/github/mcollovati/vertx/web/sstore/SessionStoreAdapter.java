package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.ClusteredSessionStoreImpl;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;

/**
 * Created by marco on 27/07/16.
 */
public class SessionStoreAdapter {

    private static final String VAADIN_SESSION_EXPIRED_ADDRESS = "vaadin.session.expired";

    public static SessionStore adapt(VertxVaadinService vaadinService, SessionStore sessionStore) {
        MessageProducer<String> sessionMessageProducer = sessionExpiredProducer(vaadinService);
        if (sessionStore instanceof SessionExpirationNotifier) {
            return sessionStore;
        }
        if (sessionStore instanceof LocalSessionStoreImpl) {
            return withSessionExpirationHandler(
                vaadinService, LocalSessionStoreAdapter.of(sessionMessageProducer, (LocalSessionStoreImpl) sessionStore)
            );
        }
        if (sessionStore instanceof ClusteredSessionStoreImpl) {
            return withSessionExpirationHandler(vaadinService,
                new ClusteredSessionStoreAdapter(sessionMessageProducer, (ClusteredSessionStoreImpl) sessionStore, vaadinService)
            );
        }
        throw new VertxException("Cannot adapt session store of type " + sessionStore.getClass().getName());
    }

    private static <S extends SessionStore & SessionExpirationNotifier<?>> S withSessionExpirationHandler(
        VertxVaadinService service, S store
    ) {
        MessageProducer<String> sessionExpiredProducer = sessionExpiredProducer(service);

        store.expirationHandler(res -> {
            if (res.succeeded()) {
                sessionExpiredProducer.send(res.result());
            } else {
                res.cause().printStackTrace();
            }

        });
        return store;
    }

    private static MessageProducer<String> sessionExpiredProducer(VertxVaadinService vaadinService) {
        return vaadinService.getVertx().eventBus().sender(VAADIN_SESSION_EXPIRED_ADDRESS);
    }

    public static MessageConsumer<String> sessionExpiredHandler(Vertx vertx, Handler<Message<String>> handler) {
        return vertx.eventBus().consumer(VAADIN_SESSION_EXPIRED_ADDRESS, handler);
    }
}
