package com.github.mcollovati.vertx.web.sstore;

import java.util.Optional;

import com.github.mcollovati.vertx.web.ExtendedSession;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.impl.ClusteredSessionStoreImpl;
import io.vertx.spi.cluster.hazelcast.impl.HazelcastAsyncMap;
import org.fest.reflect.core.Reflection;

/**
 * Created by marco on 27/07/16.
 */
public class ClusteredSessionStoreAdapter implements ClusteredSessionStore {

    private final MessageProducer<String> sessionExpiredProducer;
    private final ClusteredSessionStoreImpl sessionStore;
    private Runnable listenerCleaner;

    public ClusteredSessionStoreAdapter(MessageProducer<String> sessionExpiredProducer, ClusteredSessionStoreImpl sessionStore) {
        this.sessionExpiredProducer = sessionExpiredProducer;
        this.sessionStore = sessionStore;
    }

    @Override
    public long retryTimeout() {
        return sessionStore.retryTimeout();
    }

    @Override
    public ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout));
    }

    @Override
    public ExtendedSession createSession(long timeout, int length) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout, length));
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        sessionStore.get(id, event -> resultHandler.handle(event.map(ExtendedSession::adapt)));
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        sessionStore.delete(id, resultHandler);
    }

    @Override
    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
        ExtendedSession.adapt(session).withStandardSession(s ->
            sessionStore.put(s, e -> {
                adaptListener();
                resultHandler.handle(e);
            }));
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
        Optional.ofNullable(listenerCleaner).ifPresent(Runnable::run);
        sessionStore.close();
    }

    @SuppressWarnings("unchecked")
    private synchronized void adaptListener() {
        if (listenerCleaner == null) {
            // TODO - move in separated jar as some sort of provider
            AsyncMap<String, Session> map = Reflection.field("sessionMap").ofType(AsyncMap.class).in(sessionStore).get();
            String listenerId = tryGetHazelcastMap(map)
                .map(imap -> imap.addEntryListener(new MapListenerAdapter<String, Session>() {
                    @Override
                    public void entryExpired(EntryEvent<String, Session> event) {
                        sessionExpiredProducer.send(event.getKey());
                    }
                }, true)).orElse(null);
            listenerCleaner = () -> tryGetHazelcastMap(map).ifPresent(imap -> imap.removeEntryListener(listenerId));
        }
    }

    // TODO - move in separated jar as some sort of provider
    private Optional<IMap> tryGetHazelcastMap(AsyncMap<String, Session> map) {
        return Optional.ofNullable(map)
            .map(m -> Reflection.field("delegate").ofType(AsyncMap.class).in(m).get())
            .filter(HazelcastAsyncMap.class::isInstance)
            .map(HazelcastAsyncMap.class::cast)
            .map(h -> Reflection.field("map").ofType(IMap.class).in(h).get());
    }


}
