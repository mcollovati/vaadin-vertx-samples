package com.github.mcollovati.vertx.web.sstore;

import java.util.Objects;
import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;

class ExtendedLocalSessionStoreImpl implements ExtendedLocalSessionStore,
    SessionExpirationNotifier<ExtendedLocalSessionStore> {


    private final LocalMap<String, Session> localMap;
    private final LocalSessionStore sessionsStore;
    private Handler<AsyncResult<String>> expirationHandler = x -> {};

    public ExtendedLocalSessionStoreImpl(Vertx vertx, String sessionMapName, long reaperInterval) {
        this.localMap = vertx.sharedData().getLocalMap(sessionMapName);
        this.sessionsStore = new LocalSessionStoreImpl(vertx, sessionMapName, reaperInterval) {
            @Override
            public synchronized void handle(Long tid) {
                notifyExpiredSessions(() -> super.handle(tid));
            }
        };
    }

    @Override
    public ExtendedLocalSessionStoreImpl expirationHandler(Handler<AsyncResult<String>> handler) {
        this.expirationHandler = Objects.requireNonNull(handler);
        return this;
    }

    @Override
    public long retryTimeout() {
        return sessionsStore.retryTimeout();
    }

    @Override
    public Session createSession(long timeout) {
        return sessionsStore.createSession(timeout);
    }

    @Override
    public Session createSession(long timeout, int length) {
        return sessionsStore.createSession(timeout, length);
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        sessionsStore.get(id, resultHandler);
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Void>> resultHandler) {
        sessionsStore.delete(id, resultHandler);
    }


    @Override
    public void put(Session session, Handler<AsyncResult<Void>> resultHandler) {
        sessionsStore.put(session, resultHandler);
    }

    @Override
    public void clear(Handler<AsyncResult<Void>> resultHandler) {
        sessionsStore.clear(resultHandler);
    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        sessionsStore.size(resultHandler);
    }

    @Override
    public void close() {
        sessionsStore.close();
    }

    private void notifyExpiredSessions(Runnable reaper) {
        Set<String> before = localMap.keySet();
        reaper.run();
        before.removeAll(localMap.keySet());
        before.forEach(this::onSessionExpired);
    }

    protected void onSessionExpired(String sessionId) {
        try {
            expirationHandler.handle(Future.succeededFuture(sessionId));
        } catch (Exception ex) {
            expirationHandler.handle(Future.failedFuture(ex));
        }
    }

}
