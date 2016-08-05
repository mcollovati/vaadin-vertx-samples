package com.github.mcollovati.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.Session;

/**
 * Created by marco on 27/07/16.
 */
public interface ExtendedSession extends Session {

    /**
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return a <code>long</code> specifying when this session was created, expressed in
     * milliseconds since 1/1/1970 GMT
     */
    long createdAt();

    /**
     * Add a handler that will be called after session expires.
     *
     * @param handler  the handler
     * @return  the id of the handler. This can be used if you later want to remove the handler.
     */
    int addExpiredHandler(Handler<ExtendedSession> handler);

    /**
     * Remove a session espired handler
     *
     * @param handlerID  the id as returned from {@link #addExpiredHandler(Handler)}.
     * @return true if the handler existed and was removed, false otherwise
     */
    boolean removeHeadersEndHandler(int handlerID);




    static ExtendedSession adapt(Session session) {
        if (session instanceof ExtendedSession) {
            return (ExtendedSession) session;
        }
        return new ExtendedSessionImpl(session);
    }
}
