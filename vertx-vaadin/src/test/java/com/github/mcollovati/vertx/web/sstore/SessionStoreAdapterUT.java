package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by marco on 27/07/16.
 */
@RunWith(VertxUnitRunner.class)
public class SessionStoreAdapterUT {

    Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    @Test
    public void shouldAdaptLocalSessionStore() {
        long before = Instant.now().toEpochMilli();
        SessionStore adapted = SessionStoreAdapter.adapt(LocalSessionStore.create(vertx));
        assertThat(adapted).isInstanceOf(LocalSessionStoreAdapter.class);
        Session session = adapted.createSession(1000);
        assertThat(session).isInstanceOf(ExtendedSession.class);
        ExtendedSession extendedSession = (ExtendedSession) session;
        assertThat(extendedSession.createdAt()).isBetween(before, Instant.now().toEpochMilli());
    }

    @Test
    public void shouldAdaptClusteredLocalSessionStore() {
        long before = Instant.now().toEpochMilli();
        SessionStore adapted = SessionStoreAdapter.adapt(ClusteredSessionStore.create(vertx));
        assertThat(adapted).isInstanceOf(ClusteredSessionStoreAdapter.class);
        Session session = adapted.createSession(1000);
        assertThat(session).isInstanceOf(ExtendedSession.class);
        ExtendedSession extendedSession = (ExtendedSession) session;
        assertThat(extendedSession.createdAt()).isBetween(before, Instant.now().toEpochMilli());
    }
}
