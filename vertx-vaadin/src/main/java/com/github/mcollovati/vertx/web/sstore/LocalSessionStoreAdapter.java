package com.github.mcollovati.vertx.web.sstore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.impl.LocalSessionStoreImpl;
import org.fest.reflect.core.Reflection;

/**
 * Created by marco on 27/07/16.
 */
public class LocalSessionStoreAdapter extends LocalSessionStoreImpl implements SessionExpirationNotifier<LocalSessionStore> {


    //private final MessageProducer<String> sessionExpiredProducer;
    private LocalMap<String, Session> localMap;
    private Handler<String> expirationHandler = id -> {};


    public LocalSessionStoreAdapter(Vertx vertx, String sessionMapName, long reaperInterval, MessageProducer<String> sessionExpiredProducer) {
        super(vertx, sessionMapName, reaperInterval);
        //this.sessionExpiredProducer = Objects.requireNonNull(sessionExpiredProducer);
        localMap = vertx.sharedData().getLocalMap(sessionMapName);
    }

    @Override
    public ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(super.createSession(timeout));
    }

    public void handle(Long tid) {
        Map<String, Session> copy = new HashMap<>();
        localMap.keySet().forEach(k -> copy.put(k, localMap.get(k)));
        super.handle(tid);
        localMap.keySet().forEach(copy::remove);
        Future f = Future.future();
        copy.values().stream().map(Session::id)
            .forEach(expirationHandler::handle);
        //.forEach(sessionExpiredProducer::send);
        copy.clear();
    }

    @Override
    public LocalSessionStoreAdapter expirationHandler(Handler<String> handler) {
        this.expirationHandler = Objects.requireNonNull(handler);
        return this;
    }


    @SuppressWarnings("unchecked")
    static LocalSessionStoreAdapter of(MessageProducer<String> sessionExpiredProducer, LocalSessionStoreImpl delegate) {
        LocalMap<String, Session> localMap = Reflection.field("localMap").ofType(LocalMap.class).in(delegate).get();
        String sessionMapName = Reflection.field("name").ofType(String.class).in(localMap).get();
        Vertx vertx = Reflection.field("vertx").ofType(Vertx.class).in(delegate).get();
        long reaperInterval = Reflection.field("reaperInterval").ofType(long.class).in(delegate).get();
        delegate.close();
        return new LocalSessionStoreAdapter(vertx, sessionMapName, reaperInterval, sessionExpiredProducer);
    }

}
