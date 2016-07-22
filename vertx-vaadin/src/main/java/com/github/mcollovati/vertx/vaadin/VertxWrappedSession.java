package com.github.mcollovati.vertx.vaadin;

import com.vaadin.server.WrappedSession;
import io.vertx.ext.web.Session;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Created by marco on 16/07/16.
 */
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class VertxWrappedSession implements WrappedSession {


    private final Session delegate;

    public Session getVertxSession() {
        return delegate;
    }

    @Override
    public int getMaxInactiveInterval() {
        return Long.valueOf(delegate.timeout()).intValue();
    }

    @Override
    public Object getAttribute(String name) {
        return delegate.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        delegate.put(name, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        return delegate.data().keySet();
    }

    @Override
    public void invalidate() {
        delegate.destroy();
    }

    @Override
    public String getId() {
        return delegate.id();
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public long getLastAccessedTime() {
        return delegate.lastAccessed();
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public void removeAttribute(String name) {
        delegate.remove(name);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        // TODO
    }
}
