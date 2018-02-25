package com.github.mcollovati.vertx.web.sstore;

import java.util.Optional;

import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertx.vaadin.VertxWrappedSession;
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
    private final VertxVaadinService vaadinService;
    private Runnable listenerCleaner;

    public ClusteredSessionStoreAdapter(MessageProducer<String> sessionExpiredProducer, ClusteredSessionStoreImpl sessionStore,
                                        VertxVaadinService vaadinService) {
        this.sessionExpiredProducer = sessionExpiredProducer;
        this.sessionStore = sessionStore;
        this.vaadinService = vaadinService;
    }

    @SuppressWarnings("unchecked")
    private synchronized void adaptListener() {
        if (listenerCleaner == null) {
            // TODO - move in separated jar as some sort of provider
            AsyncMap<String, Session> map = Reflection.field("sessionMap").ofType(AsyncMap.class).in(sessionStore).get();
            Optional<IMap> hazelcastMap = tryGetHazelcastMap(map);
            String listenerId = hazelcastMap
                .map(imap -> imap.addEntryListener(new MapListenerAdapter<String, Session>() {
                    @Override
                    public void entryExpired(EntryEvent<String, Session> event) {
                        sessionExpiredProducer.send(event.getKey());
                    }

                    @Override
                    public void entryMerged(EntryEvent<String, Session> event) {
                        vaadinService.loadSession(new VertxWrappedSession(ExtendedSession.adapt(event.getValue())));
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

    @Override
    public long retryTimeout() {
        return sessionStore.retryTimeout();
    }

    @Override
    public ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout));
    }

    @Override
    public Session createSession(long timeout, int length) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout, length));
    }

    @Override
    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        Handler<AsyncResult<Session>> refreshTransientAndDelegate = event -> {
            if (event.succeeded() && event.result() != null) {
                vaadinService.loadSession(new VertxWrappedSession(ExtendedSession.adapt(event.result())));
            }
            resultHandler.handle(event);
        };
        sessionStore.get(id, refreshTransientAndDelegate);
    }

    @Override
    public void delete(String id, Handler<AsyncResult<Void>> resultHandler) {
        sessionStore.delete(id, resultHandler);
    }

    @Override
    public void put(Session session, Handler<AsyncResult<Void>> resultHandler) {
        sessionStore.put(session, e -> {
            adaptListener();
            resultHandler.handle(e);
        });
    }

    @Override
    public void clear(Handler<AsyncResult<Void>> resultHandler) {
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


}
