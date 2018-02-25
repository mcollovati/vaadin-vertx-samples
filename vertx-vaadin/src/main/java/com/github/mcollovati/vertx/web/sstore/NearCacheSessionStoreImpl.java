package com.github.mcollovati.vertx.web.sstore;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;

class NearCacheSessionStoreImpl implements NearCacheSessionStore {


    private final Vertx vertx;
    private final long reaperInterval;
    private final LocalMap<String, Session> localMap;
    private final ClusteredSessionStore clusteredSessionStore;
    private volatile AsyncMap<String, Session> remoteMap;

    public NearCacheSessionStoreImpl(Vertx vertx, String sessionMapName, long retryTimeout) {
        this.vertx = vertx;
        this.reaperInterval = 1000L; // reaperInterval;
        this.clusteredSessionStore = ClusteredSessionStore.create(vertx, sessionMapName, retryTimeout);
        this.localMap = vertx.sharedData().getLocalMap(sessionMapName);
        setTimer();
    }


    @Override
    public long retryTimeout() {
        return clusteredSessionStore.retryTimeout();
    }

    @Override
    public Session createSession(long timeout) {
        return clusteredSessionStore.createSession(timeout);
    }

    @Override
    public Session createSession(long timeout, int length) {
        return clusteredSessionStore.createSession(timeout, length);
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {

    }

    @Override
    public void delete(String id, Handler<AsyncResult<Void>> resultHandler) {

    }


    @Override
    public void put(Session session, Handler<AsyncResult<Void>> resultHandler) {
        clusteredSessionStore.put(session, res -> {
            if (res.succeeded()) {
                localMap.put(session.id(), session);
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void clear(Handler<AsyncResult<Void>> resultHandler) {

    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {

    }

    @Override
    public void close() {

    }

    private void setTimer() {

    }


}
