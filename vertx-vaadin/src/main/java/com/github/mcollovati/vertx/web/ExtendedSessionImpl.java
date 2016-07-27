package com.github.mcollovati.vertx.web;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.impl.SessionImpl;

import java.util.Map;

/**
 * Created by marco on 27/07/16.
 */
public class ExtendedSessionImpl implements ExtendedSession, Shareable, ClusterSerializable {

    protected Session delegate;
    private long createdAt;

    public ExtendedSessionImpl() {
        this.delegate = new SessionImpl();
    }

    public ExtendedSessionImpl(Session delegate) {
        this.delegate = delegate;
        this.createdAt = System.currentTimeMillis();
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
        ((ClusterSerializable)delegate).writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        createdAt = buffer.getLong(pos);
        return ((ClusterSerializable)delegate).readFromBuffer(pos + 8, buffer);
    }
}
