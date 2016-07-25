/*
 * The MIT License
 * Copyright © 2016 Marco Collovati (mcollovati@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mcollovati.vertx.vaadin;

import com.vaadin.server.WrappedSession;
import io.vertx.ext.web.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.atmosphere.vertx.VertxHttpSession;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

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
    public void setMaxInactiveInterval(int interval) {
        // TODO
    }

    @Override
    public Object getAttribute(String name) {
        checkSessionState();
        return delegate.get(name);
    }

    @Override
    // TODO: fire HttpSessionAttributeListener
    public void setAttribute(String name, Object value) {
        checkSessionState();
        if (value == null) {
            removeAttribute(name);
        } else {
            tryCastHttpSessionBindingListener(delegate.get(name))
                .ifPresent(oldValue -> oldValue.valueUnbound(createHttpSessionBindingEvent(name, oldValue)));
            delegate.put(name, value);
            tryCastHttpSessionBindingListener(value)
                .ifPresent(listener -> listener.valueBound(createHttpSessionBindingEvent(name, value)));
        }
    }

    private Optional<HttpSessionBindingListener> tryCastHttpSessionBindingListener(Object value) {
        return Optional.ofNullable(value)
            .filter(HttpSessionBindingListener.class::isInstance)
            .map(HttpSessionBindingListener.class::cast);
    }

    private HttpSessionBindingEvent createHttpSessionBindingEvent(String name, Object value) {
        return new HttpSessionBindingEvent(new VertxHttpSession(this), name, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        checkSessionState();
        return delegate.data().keySet();
    }

    @Override
    // TODO: catch HttpSessionBindingListener exceptions?
    public void invalidate() {
        checkSessionState();
        Map<String, HttpSessionBindingListener> toUnbind = delegate.data().entrySet().stream()
            .filter( entry -> HttpSessionBindingListener.class.isInstance(entry.getValue()))
            .collect(toMap(e -> e.getKey(), e -> HttpSessionBindingListener.class.cast(e.getValue())));
        delegate.destroy();
        toUnbind.forEach( (name, listener) -> listener.valueUnbound(createHttpSessionBindingEvent(name,listener)));
        toUnbind.clear();
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

    private void checkSessionState() {
        if (delegate.isDestroyed()) {
            throw new IllegalStateException("Session already invalidated");
        }
    }

}
