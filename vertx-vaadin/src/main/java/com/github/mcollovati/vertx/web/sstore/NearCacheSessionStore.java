package com.github.mcollovati.vertx.web.sstore;

import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.SessionStore;

public interface NearCacheSessionStore extends SessionStore, SessionExpirationNotifier<NearCacheSessionStore> {
    /**
     * The default name used for the session map
     */
    String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";

    /**
     * Default retry time out, in ms, for a session not found in this store.
     */
    long DEFAULT_RETRY_TIMEOUT = 5 * 1000; // 5 seconds

    /**
     * Default of how often, in ms, to check for expired sessions
     */
    long DEFAULT_REAPER_INTERVAL = 1000;

    /**
     * Create a session store
     *
     * @param vertx          the Vert.x instance
     * @param sessionMapName the session map name
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx, String sessionMapName) {
        return new NearCacheSessionStoreImpl(vertx, sessionMapName, DEFAULT_RETRY_TIMEOUT, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store.<p/>
     *
     * The retry timeout value, configures how long the session handler will retry to get a session from the store
     * when it is not found.
     *
     * @param vertx          the Vert.x instance
     * @param sessionMapName the session map name
     * @param retryTimeout   the store retry timeout, in ms
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx, String sessionMapName, long retryTimeout) {
        return new NearCacheSessionStoreImpl(vertx, sessionMapName, retryTimeout, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store.<p/>
     *
     * The retry timeout value, configures how long the session handler will retry to get a session from the store
     * when it is not found.
     * The reaper interval configures how often, in ms, to check for expired sessions
     *
     * @param vertx          the Vert.x instance
     * @param sessionMapName the session map name
     * @param retryTimeout   the store retry timeout, in ms
     * @param reaperInterval how often, in ms, to check for expired sessions
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx, String sessionMapName, long retryTimeout, long reaperInterval) {
        return new NearCacheSessionStoreImpl(vertx, sessionMapName, retryTimeout, reaperInterval);
    }

    /**
     * Create a session store
     *
     * @param vertx the Vert.x instance
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx) {
        return new NearCacheSessionStoreImpl(vertx, DEFAULT_SESSION_MAP_NAME, DEFAULT_RETRY_TIMEOUT, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store.<p/>
     *
     * The retry timeout value, configures how long the session handler will retry to get a session from the store
     * when it is not found.
     *
     * @param vertx        the Vert.x instance
     * @param retryTimeout the store retry timeout, in ms
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx, long retryTimeout) {
        return new NearCacheSessionStoreImpl(vertx, DEFAULT_SESSION_MAP_NAME, retryTimeout, DEFAULT_REAPER_INTERVAL);
    }

    /**
     * Create a session store.<p/>
     *
     * The retry timeout value, configures how long the session handler will retry to get a session from the store
     * when it is not found.
     * The reaper interval configures how often, in ms, to check for expired sessions.
     *
     * @param vertx          the Vert.x instance
     * @param retryTimeout   the store retry timeout, in ms
     * @param reaperInterval how often, in ms, to check for expired sessions
     * @return the session store
     */
    static NearCacheSessionStore create(Vertx vertx, long retryTimeout, long reaperInterval) {
        return new NearCacheSessionStoreImpl(vertx, DEFAULT_SESSION_MAP_NAME, retryTimeout, reaperInterval);
    }
}
