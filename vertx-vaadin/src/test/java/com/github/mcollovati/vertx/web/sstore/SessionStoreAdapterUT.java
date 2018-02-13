package com.github.mcollovati.vertx.web.sstore;

import java.time.Instant;
import java.util.Optional;

import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by marco on 27/07/16.
 */
@RunWith(VertxUnitRunner.class)
public class SessionStoreAdapterUT {

    Vertx vertx;
    VertxVaadinService vertxVaadinService;
    MessageConsumer<String> sessionExpiredConsumer;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertxVaadinService = Mockito.mock(VertxVaadinService.class);
        when(vertxVaadinService.getVertx()).thenAnswer(i -> vertx);
    }

    @After
    public void tearDown(TestContext context) {
        Optional.ofNullable(sessionExpiredConsumer).ifPresent(MessageConsumer::unregister);
        vertx.close(context.asyncAssertSuccess());
    }


    @Test
    public void shouldAdaptLocalSessionStore() {
        long before = Instant.now().toEpochMilli();
        SessionStore adapted = SessionStoreAdapter.adapt(vertxVaadinService, LocalSessionStore.create(vertx));
        assertThat(adapted).isInstanceOf(LocalSessionStoreAdapter.class);
        Session session = adapted.createSession(1000);
        assertThat(session).isInstanceOf(ExtendedSession.class);
        ExtendedSession extendedSession = (ExtendedSession) session;
        assertThat(extendedSession.createdAt()).isBetween(before, Instant.now().toEpochMilli());
    }

    @Test(timeout = 5000L)
    public void shouldFireSessionExpiredEventForLocalSessionStore(TestContext context) {
        Async async = context.async();
        SessionStore adapted = SessionStoreAdapter.adapt(vertxVaadinService, LocalSessionStore.create(vertx));
        Session session = adapted.createSession(1000);

        sessionExpiredConsumer = SessionStoreAdapter.sessionExpiredHandler(vertx, event -> {
            context.assertEquals(session.id(), event.body());
            async.countDown();
        });
        adapted.put(session, Future.<Boolean>future().completer());
        session.put("a", "b");
    }


    @Test
    public void shouldAdaptClusteredLocalSessionStore() {
        long before = Instant.now().toEpochMilli();
        SessionStore adapted = SessionStoreAdapter.adapt(vertxVaadinService, ClusteredSessionStore.create(vertx));
        assertThat(adapted).isInstanceOf(ClusteredSessionStoreAdapter.class);
        Session session = adapted.createSession(1000);
        assertThat(session).isInstanceOf(ExtendedSession.class);
        ExtendedSession extendedSession = (ExtendedSession) session;
        assertThat(extendedSession.createdAt()).isBetween(before, Instant.now().toEpochMilli());
    }

    @Test(timeout = 15000L)
    public void shouldFireSessionExpiredEventForHazelcastSessionStore(TestContext context) {
        Async async = context.async();
        Vertx.clusteredVertx(new VertxOptions().setClusterManager(new HazelcastClusterManager()), res -> {

            vertx = res.result();
            SessionStore adapted = SessionStoreAdapter.adapt(vertxVaadinService, ClusteredSessionStore.create(vertx));
            Session session = adapted.createSession(2000);

            sessionExpiredConsumer = SessionStoreAdapter.sessionExpiredHandler(vertx, event -> {
                context.assertEquals(session.id(), event.body());
                async.countDown();
            });
            adapted.put(session, Future.<Boolean>future().completer());
            session.put("a", "b");

        });

    }


}
