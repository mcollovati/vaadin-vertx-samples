package com.github.mcollovati.vertx.web.sstore;

import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.LocalSessionStore;

/**
 * A session store that extends {@link LocalSessionStore} with support
 * for expiration handler
 */
public interface ExtendedLocalSessionStore extends LocalSessionStore,
    SessionExpirationNotifier<ExtendedLocalSessionStore> {

    /**
     * Default of how often, in ms, to check for expired sessions
     */
    long DEFAULT_REAPER_INTERVAL = 1000;

    /**
     * Default name for map used to store sessions
     */
    String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";

    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @return the session store
     */
    static ExtendedLocalSessionStore create(Vertx vertx) {
        return new ExtendedLocalSessionStoreImpl(vertx, DEFAULT_SESSION_MAP_NAME, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @param sessionMapName  name for map used to store sessions
     * @return the session store
     */
    static ExtendedLocalSessionStore create(Vertx vertx, String sessionMapName) {
        return new ExtendedLocalSessionStoreImpl(vertx, sessionMapName, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store
     *
     * @param vertx  the Vert.x instance
     * @param sessionMapName  name for map used to store sessions
     * @param reaperInterval  how often, in ms, to check for expired sessions
     * @return the session store
     */
    static ExtendedLocalSessionStore create(Vertx vertx, String sessionMapName, long reaperInterval) {
        return new ExtendedLocalSessionStoreImpl(vertx, sessionMapName, reaperInterval);
    }

}
