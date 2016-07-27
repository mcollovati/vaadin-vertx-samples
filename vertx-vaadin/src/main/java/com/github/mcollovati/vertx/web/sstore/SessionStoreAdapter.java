package com.github.mcollovati.vertx.web.sstore;

import io.vertx.core.VertxException;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.ClusteredSessionStoreImpl;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;

/**
 * Created by marco on 27/07/16.
 */
public class SessionStoreAdapter {

    public static SessionStore adapt(SessionStore sessionStore) {
        if (sessionStore instanceof LocalSessionStoreImpl) {
            return new LocalSessionStoreAdapter((LocalSessionStoreImpl) sessionStore);
        }
        if (sessionStore instanceof ClusteredSessionStoreImpl) {
            return new ClusteredSessionStoreAdapter((ClusteredSessionStoreImpl)sessionStore);
        }
        throw new VertxException("Cannot adapt session store of type " + sessionStore.getClass().getName());
    }
}
