package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.impl.ClusteredSessionStoreImpl;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Created by marco on 27/07/16.
 */
@RequiredArgsConstructor
public class ClusteredSessionStoreAdapter implements ClusteredSessionStore {

    @NonNull
    private final ClusteredSessionStoreImpl sessionStore;

    @Override
    public long retryTimeout() {
        return sessionStore.retryTimeout();
    }

    @Override
    public ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout));
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        sessionStore.get(id, resultHandler);
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        sessionStore.delete(id, resultHandler);
    }

    @Override
    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
        sessionStore.put(session, resultHandler);
    }

    @Override
    public void clear(Handler<AsyncResult<Boolean>> resultHandler) {
        sessionStore.clear(resultHandler);
    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        sessionStore.size(resultHandler);
    }

    @Override
    public void close() {
        sessionStore.close();
    }


}
