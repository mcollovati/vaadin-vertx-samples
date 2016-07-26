package com.github.mcollovati.vertx.vaadin;

import io.vertx.ext.web.Session;

/**
 * Created by marco on 26/07/16.
 */
public interface ExtendedSession extends Session {

    long createdAt();

    ExtendedSession timeout(long timeout);

}
