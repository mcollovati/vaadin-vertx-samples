package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Created by marco on 27/07/16.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LocalSessionStoreAdapter implements LocalSessionStore {

    @NonNull
    private final LocalSessionStoreImpl delegate;

    @Override
    public ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(delegate.createSession(timeout));
    }

    @Override
    public long retryTimeout() {
        return delegate.retryTimeout();
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        delegate.get(id, resultHandler);
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        delegate.delete(id, resultHandler);
    }

    @Override
    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
        delegate.put(session, resultHandler);
    }

    @Override
    public void clear(Handler<AsyncResult<Boolean>> resultHandler) {
        delegate.clear(resultHandler);
    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        delegate.size(resultHandler);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public void handle(Long tid) {
        delegate.handle(tid);
    }


}
