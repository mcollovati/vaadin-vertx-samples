package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.web.ExtendedSession;
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

    public static SessionStore adapt(Vertx vertx, SessionStore sessionStore) {
        MessageProducer<String> sessionMessageProducer = vertx.eventBus().sender(VAADIN_SESSION_EXPIRED_ADDRESS);
        if (sessionStore instanceof LocalSessionStoreImpl) {
            return LocalSessionStoreAdapter.of(sessionMessageProducer, (LocalSessionStoreImpl) sessionStore);
        }
        if (sessionStore instanceof ClusteredSessionStoreImpl) {
            return new ClusteredSessionStoreAdapter(sessionMessageProducer, (ClusteredSessionStoreImpl) sessionStore);
        }
        throw new VertxException("Cannot adapt session store of type " + sessionStore.getClass().getName());
    }

    public static MessageConsumer<String> sessionExpiredHandler(Vertx vertx, Handler<Message<String>> handler) {
        return vertx.eventBus().consumer(VAADIN_SESSION_EXPIRED_ADDRESS, handler);
    }
}
