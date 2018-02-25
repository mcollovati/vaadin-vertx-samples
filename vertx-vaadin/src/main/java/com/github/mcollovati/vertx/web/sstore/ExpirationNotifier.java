package com.github.mcollovati.vertx.web.sstore;

import io.vertx.core.Handler;
import io.vertx.ext.web.sstore.SessionStore;

public interface ExpirationNotifier<T extends SessionStore> {

    /**
     * Set an expiration handler on this {@link SessionStore}.
     *
     * Once a session expires {@code handler} will be called
     * with the session id.
     *
     * @param handler The session expiration handler
     * @return a reference to this, so the API can be used fluently
     */
    T expirationHandler(Handler<String> handler);
}
