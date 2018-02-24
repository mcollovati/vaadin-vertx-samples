package com.github.mcollovati.vertx.vaadin.communication;

import java.io.Serializable;
import java.util.concurrent.CompletionStage;

interface PushSocket extends Serializable {

    String getUUID();

    String remoteAddress();

    CompletionStage<?> send(String message);

    CompletionStage<?> close();

    boolean isConnected();

}
