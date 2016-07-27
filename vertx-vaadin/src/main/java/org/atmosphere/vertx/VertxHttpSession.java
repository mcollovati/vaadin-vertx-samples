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
package org.atmosphere.vertx;

import com.github.mcollovati.vertx.vaadin.VertxWrappedSession;
import com.github.mcollovati.vertx.web.ExtendedSession;
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

    VertxHttpSession(ExtendedSession session) {
        this(new VertxWrappedSession(Objects.requireNonNull(session)));
    }

    public VertxHttpSession(VertxWrappedSession session) {
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

    private interface Exclusions {
        Set<String> getAttributeNames();
    }

}
