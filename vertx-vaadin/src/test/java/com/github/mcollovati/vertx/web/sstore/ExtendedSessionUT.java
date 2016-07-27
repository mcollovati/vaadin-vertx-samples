package com.github.mcollovati.vertx.web.sstore;

import com.github.mcollovati.vertx.web.ExtendedSession;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.web.sstore.impl.SessionImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by marco on 27/07/16.
 */
public class ExtendedSessionUT {

    @Test
    public void extendeSessionShouldBeClusterSerializable() throws InterruptedException {
        SessionImpl delegate = new SessionImpl(3000);
        ExtendedSession extendedSession = ExtendedSession.adapt(delegate);
        assertThat(extendedSession).isInstanceOf(ClusterSerializable.class);
        long createdAt = extendedSession.createdAt();
        extendedSession.put("key1", "value");
        extendedSession.put("key2", 20);
        Thread.sleep(300);

        Buffer buffer = Buffer.buffer();
        ((ClusterSerializable) extendedSession).writeToBuffer(buffer);
        assertThat(buffer.length() > 0);

        ExtendedSession fromBuffer = ExtendedSession.adapt(new SessionImpl(0));
        ((ClusterSerializable) fromBuffer).readFromBuffer(0, buffer);
        assertThat(fromBuffer.createdAt()).isEqualTo(createdAt);
        assertThat(fromBuffer.id()).isEqualTo(delegate.id());
        assertThat(fromBuffer.timeout()).isEqualTo(delegate.timeout());
        assertThat(fromBuffer.data()).isEqualTo(delegate.data());

    }
}
