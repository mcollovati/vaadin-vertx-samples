package org.atmosphere.vertx;

import com.github.mcollovati.vertx.vaadin.VertxWrappedSession;
import io.vertx.ext.web.Session;
import lombok.experimental.Delegate;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;

/**
 * Created by marco on 19/07/16.
 */
public class VertxHttpSession implements HttpSession {

    @Delegate(excludes = Exclusions.class)
    VertxWrappedSession delegate;

    private interface Exclusions {
        Set<String> getAttributeNames();
    }

    VertxHttpSession(Session session) {
        this(new VertxWrappedSession(Objects.requireNonNull(session)));
    }
    VertxHttpSession(VertxWrappedSession session) {
        this.delegate = Objects.requireNonNull(session);
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public Object getValue(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(delegate.getAttributeNames());
    }

    @Override
    public String[] getValueNames() {
        return delegate.getAttributeNames().toArray(new String[0]);
    }

    @Override
    public void putValue(String name, Object value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeValue(String name) {
        delegate.removeAttribute(name);
    }

}
