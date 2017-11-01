package com.github.mcollovati.vertx.web;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.impl.SessionImpl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by marco on 27/07/16.
 */
public class ExtendedSessionImpl implements ExtendedSession, Shareable, ClusterSerializable {

    protected Session delegate;
    private long createdAt;
    private Map<Integer, Handler<Void>> headersEndHandlers;
    private AtomicInteger handlerSeq = new AtomicInteger();

    public ExtendedSessionImpl() {
        this.delegate = new SessionImpl();
    }

    public ExtendedSessionImpl(Session delegate) {
        this.delegate = delegate;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public int addExpiredHandler(Handler<ExtendedSession> handler) {
        return 0;
    }

    @Override
    public boolean removeHeadersEndHandler(int handlerID) {
        return false;
    }

    @Override
    public Session regenerateId() {
        return delegate.regenerateId();
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public Session put(String key, Object obj) {
        return delegate.put(key, obj);
    }

    @Override
    public <T> T get(String key) {
        return delegate.get(key);
    }

    @Override
    public <T> T remove(String key) {
        return delegate.remove(key);
    }

    @Override
    public Map<String, Object> data() {
        return delegate.data();
    }

    @Override
    public long lastAccessed() {
        return delegate.lastAccessed();
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return delegate.isDestroyed();
    }

    @Override
    public boolean isRegenerated() {
        return delegate.isRegenerated();
    }

    @Override
    public String oldId() {
        return delegate.oldId();
    }

    @Override
    public long timeout() {
        return delegate.timeout();
    }

    @Override
    public void setAccessed() {
        delegate.setAccessed();
    }

    @Override
    public long createdAt() {
        return createdAt;
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        buffer.appendLong(createdAt);
        ((ClusterSerializable) delegate).writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        createdAt = buffer.getLong(pos);
        return ((ClusterSerializable) delegate).readFromBuffer(pos + 8, buffer);
    }

    @Override
    public void withStandardSession(Consumer<Session> consumer) {
        consumer.accept(delegate);
    }
}
