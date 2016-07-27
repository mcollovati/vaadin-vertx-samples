package com.github.mcollovati.vertx.vaadin;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * Created by marco on 26/07/16.
 */
@RunWith(VertxUnitRunner.class)
@Ignore
public class VertxWrappedSessionWithClusteredSessionStoreUT {

    Vertx vertx;
    ClusteredSessionStore sessionStore;


    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        sessionStore = ClusteredSessionStore.create(vertx);
    }

    @After
    public void tearDown(TestContext context) {
        sessionStore.close();
        vertx.close(context.asyncAssertSuccess());
    }

    @Test(timeout = 5000L)
    public void shouldInvokeBindingListener(TestContext context) {
        final Async async = context.async(2);
        VertxWrappedSession session = new VertxWrappedSession(createSession(10000));
        Listener listener1 = new Listener(async);
        session.setAttribute("key", listener1);
        session.removeAttribute("key");
    }

    private ExtendedSession createSession(long timeout) {
        return ExtendedSession.adapt(sessionStore.createSession(timeout));
    }

    @Test(timeout = 5000L)
    public void shouldInvokeBindingListenerWhenReplaced(TestContext context) {
        final Async async = context.async(2);
        VertxWrappedSession session = new VertxWrappedSession(createSession(10000));
        Listener listener1 = new Listener(async);
        session.setAttribute("key", listener1);
        session.setAttribute("key", new Object());
    }
    @Test(timeout = 5000L)
    public void shouldInvokeBindingListenerWhenSessionIsInvalidated(TestContext context) {
        final Async async = context.async(2);
        VertxWrappedSession session = new VertxWrappedSession(createSession(10000));
        Listener listener1 = new Listener(async);
        session.setAttribute("key", listener1);
        session.invalidate();
    }
    @Test(timeout = 5000L)
    public void shouldInvokeBindingListenerWhenSessionExpires(TestContext context) {
        final Async async = context.async(2);
        VertxWrappedSession session = new VertxWrappedSession(createSession(3000));
        Listener listener1 = new Listener(async);
        session.setAttribute("key", listener1);
    }



    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Listener implements HttpSessionBindingListener {

        final Async async;

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            async.countDown();
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            async.countDown();
        }
    }



}
