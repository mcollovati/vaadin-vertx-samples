package com.github.mcollovati.vertx.web.sstore;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;

class NearCacheSessionStoreImpl implements NearCacheSessionStore, Handler<Long> {


    private final Vertx vertx;
    private final long reaperInterval;
    private final LocalMap<String, Session> localMap;
    private final ClusteredSessionStore clusteredSessionStore;
    private Handler<AsyncResult<String>> expirationHandler = x -> {};
    private long timerID = -1;
    private boolean closed;

    public NearCacheSessionStoreImpl(Vertx vertx, String sessionMapName, long retryTimeout, long reaperInterval) {
        this.vertx = vertx;
        this.reaperInterval = reaperInterval;
        this.clusteredSessionStore = ClusteredSessionStore.create(vertx, sessionMapName, retryTimeout);
        this.localMap = vertx.sharedData().getLocalMap(sessionMapName);
        this.setTimer();
    }

    @Override
    public NearCacheSessionStore expirationHandler(Handler<AsyncResult<String>> handler) {
        this.expirationHandler = Objects.requireNonNull(handler);
        return this;
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
        clusteredSessionStore.get(id, res -> {
            if (res.succeeded()) {
                Session localSession = localMap.get(id);
                if (localSession == null && res.result() != null) {
                    localMap.put(id, res.result());
                }
                resultHandler.handle(Future.succeededFuture(localMap.get(id)));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
        /*
        if (localMap.containsKey(id)) {
            resultHandler.handle(Future.succeededFuture(localMap.get(id)));
        } else {
            clusteredSessionStore.get(id, res -> {
                if (res.succeeded()) {
                    Optional.ofNullable(res.result()).ifPresent(s -> localMap.put(id, s));
                    resultHandler.handle(Future.succeededFuture(res.result()));
                } else {
                    resultHandler.handle(Future.failedFuture(res.cause()));
                }
            });
        }*/
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Void>> resultHandler) {
        clusteredSessionStore.delete(id, res -> {
            if (res.succeeded()) {
                localMap.remove(id);
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }


    @Override
    public void put(Session session, Handler<AsyncResult<Void>> resultHandler) {
        clusteredSessionStore.put(session, res -> {
            localMap.put(session.id(), session);
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void clear(Handler<AsyncResult<Void>> resultHandler) {
        clusteredSessionStore.clear(res -> {
            if (res.succeeded()) {
                localMap.clear();
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(localMap.size()));
    }

    @Override
    public synchronized void close() {
        clusteredSessionStore.close();
        localMap.close();
        if (timerID != -1) {
            vertx.cancelTimer(timerID);
        }
        closed = true;
    }

    @Override
    public synchronized void handle(Long tid) {
        long now = System.currentTimeMillis();
        Set<String> toRemove = new HashSet<>();
        for (Session session : localMap.values()) {
            if (now - session.lastAccessed() > session.timeout()) {
                toRemove.add(session.id());
            }
        }
        for (String id : toRemove) {
            delete(id, res -> expirationHandler.handle(res.map(x -> id)));
        }
        if (!closed) {
            setTimer();
        }
    }

    private void setTimer() {
        if (reaperInterval != 0) {
            timerID = vertx.setTimer(reaperInterval, this);
        }
    }


}
