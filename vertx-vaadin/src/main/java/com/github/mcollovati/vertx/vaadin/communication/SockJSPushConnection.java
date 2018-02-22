package com.github.mcollovati.vertx.vaadin.communication;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.PushConnection;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.ui.UI;
import io.vertx.core.buffer.Buffer;

public class SockJSPushConnection implements PushConnection {

    private int uiId;
    private PushSocket socket;
    private State state = State.DISCONNECTED;
    private transient Future<?> outgoingMessage;

    public SockJSPushConnection(UI ui) {
        this.uiId = ui.getUIId();
    }

    @Override
    public void push() {
        push(true);
    }

    /**
     * Pushes pending state changes and client RPC calls to the client. If
     * {@code isConnected()} is false, defers the push until a connection is
     * established.
     *
     * @param async True if this push asynchronously originates from the server,
     *              false if it is a response to a client request.
     */
    void push(boolean async) {
        if (!isConnected()) {
            if (async && state != State.RESPONSE_PENDING) {
                state = State.PUSH_PENDING;
            } else {
                state = State.RESPONSE_PENDING;
            }
        } else {
            try {
                UI ui = VaadinSession.getCurrent().getUIById(this.uiId);
                Writer writer = new StringWriter();
                new UidlWriter().write(ui, writer, async);
                sendMessage("for(;;);[{" + writer + "}]");
            } catch (Exception e) {
                throw new RuntimeException("Push failed", e);
            }
        }
    }

    private void sendMessage(String message) {
        this.outgoingMessage = socket.send(message).toCompletableFuture();
    }

    protected Reader receiveMessage(Buffer data) {
        // SockJS will always receive the whole message
        return new StringReader(data.toString());
    }


    @Override
    public void disconnect() {
        assert isConnected();
        if (socket == null) {
            // Already disconnected. Should not happen but if it does, we don't
            // want to cause NPEs
            getLogger().fine(
                "SockJSPushConnection.disconnect() called twice, this should not happen");
            return;
        }

        if (outgoingMessage != null) {
            // Wait for the last message to be sent before closing the
            // connection (assumes that futures are completed in order)
            try {
                outgoingMessage.get(1000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                getLogger().log(Level.INFO,
                    "Timeout waiting for messages to be sent to client before disconnect");
            } catch (Exception e) {
                getLogger().log(Level.INFO,
                    "Error waiting for messages to be sent to client before disconnect");
            }
            outgoingMessage = null;
        }

        this.socket.close().thenRun(this::connectionLost);
    }

    @Override
    public boolean isConnected() {
        return state == State.CONNECTED && socket != null;
    }

    void connect(PushSocket socket) {
        assert socket != null;
        assert socket != this.socket;

        if (isConnected()) {
            disconnect();
        }

        this.socket = socket;
        State oldState = state;
        state = State.CONNECTED;

        if (oldState == State.PUSH_PENDING
            || oldState == State.RESPONSE_PENDING) {
            // Sending a "response" message (async=false) also takes care of a
            // pending push, but not vice versa
            push(oldState == State.PUSH_PENDING);
        }

    }

    void connectionLost() {
        socket = null;
        if (state == State.CONNECTED) {
            // Guard against connectionLost being (incorrectly) called when
            // state is PUSH_PENDING or RESPONSE_PENDING
            // (http://dev.vaadin.com/ticket/16919)
            state = State.DISCONNECTED;
        }
    }

    PushSocket getSocket() {
        return socket;
    }

    private static Logger getLogger() {
        return Logger.getLogger(SockJSPushConnection.class.getName());
    }

    protected enum State {
        /**
         * Not connected. Trying to push will set the connection state to
         * PUSH_PENDING or RESPONSE_PENDING and defer sending the message until
         * a connection is established.
         */
        DISCONNECTED,

        /**
         * Not connected. An asynchronous push is pending the opening of the
         * connection.
         */
        PUSH_PENDING,

        /**
         * Not connected. A response to a client request is pending the opening
         * of the connection.
         */
        RESPONSE_PENDING,

        /**
         * Connected. Messages can be sent through the connection.
         */
        CONNECTED;

    }

}
