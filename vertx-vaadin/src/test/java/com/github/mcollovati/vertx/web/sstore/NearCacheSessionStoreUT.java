package com.github.mcollovati.vertx.web.sstore;

import java.io.Serializable;
import java.util.Objects;

import com.github.mcollovati.vertx.Sync;
import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SessionImpl;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.ext.web.sstore.SessionStore.DEFAULT_SESSIONID_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class NearCacheSessionStoreUT {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext(() -> Sync.await(completer -> Vertx.clusteredVertx(
        new VertxOptions().setClusterManager(new HazelcastClusterManager()), completer
    )));


    private void assertOnRemoteMap(TestContext context, Session session, Handler<AsyncResult<Session>> handler) {
        rule.vertx().sharedData().getClusterWideMap(
            NearCacheSessionStore.DEFAULT_SESSION_MAP_NAME,
            context.<AsyncMap<String, Session>>asyncAssertSuccess(x -> x.get(session.id(), handler))
        );
    }

    private void assertOnLocalMap(TestContext context, Session session, Handler<AsyncResult<Session>> handler) {
        try {
            Session localSession = (Session) rule.vertx().sharedData()
                .getLocalMap(NearCacheSessionStore.DEFAULT_SESSION_MAP_NAME).get(session.id());
            handler.handle(Future.succeededFuture(localSession));
        } catch (Exception ex) {
            handler.handle(Future.failedFuture(ex));
        }
    }


    @Test
    public void createSession(TestContext context) {

        Vertx vertx = rule.vertx();
        SessionStore sessionStore = NearCacheSessionStore.create(vertx);
        long beforeCreationTime = System.currentTimeMillis();
        Session session = sessionStore.createSession(3600);
        assertThat(session.id()).isNotEmpty();
        assertThat(session.timeout()).isEqualTo(3600);
        assertThat(session.lastAccessed()).isCloseTo(beforeCreationTime, Offset.offset(100L));
        assertThat(session.isDestroyed()).isFalse();

    }

    @Test(timeout = 150000)
    public void testPut(TestContext context) {
        Vertx vertx = rule.vertx();

        TestObject testObject = new TestObject("TestObject");
        ExtendedSession session = createSession(vertx);
        String testObjKey = "testObjKey";
        session.put(testObjKey, testObject);

        SessionStore sessionStore = NearCacheSessionStore.create(vertx);
        sessionStore.put(session, context.asyncAssertSuccess(b -> {
            assertOnRemoteMap(context, session, context.asyncAssertSuccess(s ->
                context.verify(unused -> assertSessionProperties(session, s))
            ));
            assertOnLocalMap(context, session, context.asyncAssertSuccess(s ->
                context.verify(unused -> {
                    assertSessionProperties(session, s);

                })
            ));
        }));

    }

    private ExtendedSession createSession(Vertx vertx) {
        return ExtendedSession.adapt(new SessionImpl(new PRNG(vertx), 36000, DEFAULT_SESSIONID_LENGTH));
    }

    private void assertSessionProperties(ExtendedSession session, Session rs) {
        assertThat(rs.lastAccessed()).isEqualTo(session.lastAccessed());
        assertThat(rs.id()).isEqualTo(session.id());
        assertThat(rs.isDestroyed()).isEqualTo(session.isDestroyed());
        assertThat(rs.timeout()).isEqualTo(session.timeout());
    }


    private static class TestObject implements Serializable {
        private final String name;
        private int counter = 0;

        public TestObject(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestObject)) return false;
            TestObject that = (TestObject) o;
            return counter == that.counter &&
                Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(counter, name);
        }
    }
}
