package com.github.mcollovati.vertx.vaadin.communication;

import java.io.Serializable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

interface PushSocket extends Serializable {

    String getUUID();

    String remoteAddress();

    CompletionStage<?> send(String message);

    CompletionStage<?> close();


    enum Action {
        SEND {
            @Override
            void onAction(SockJSSocket socket, JsonObject payload) {
                socket.write(Buffer.buffer(payload.getString("message")));
            }
        }, CLOSE {
            @Override
            void onAction(SockJSSocket socket, JsonObject payload) {
                socket.close();
            }
        };

        abstract void onAction(SockJSSocket socket, JsonObject payload);

        JsonObject toJsonObject() {
            return new JsonObject().put("action", this);
        }

        static Action fromJsonObject(JsonObject payload) {
            return Action.valueOf(payload.getString("action"));
        }

    }

    interface Command {
        void doWithSocket(Function<String, SockJSSocket> socketSupplier);

        static Command sendMessage(String socketId, Buffer message) {
            return f -> f.apply(socketId).write(message);
        }

        static Command close(String socketId) {
            return f -> f.apply(socketId).close();
        }
    }

}
