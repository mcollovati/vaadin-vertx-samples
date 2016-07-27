package com.github.mcollovati.vertx.web;

import io.vertx.ext.web.Session;

/**
 * Created by marco on 27/07/16.
 */
public interface ExtendedSession extends Session {

    long createdAt();

    static ExtendedSession adapt(Session session) {
        if (session instanceof ExtendedSession) {
            return (ExtendedSession)session;
        }
        return new ExtendedSessionImpl(session);
    }
}
