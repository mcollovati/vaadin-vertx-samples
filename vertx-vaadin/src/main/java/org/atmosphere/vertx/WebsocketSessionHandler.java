package org.atmosphere.vertx;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import lombok.Builder;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.BiConsumer;

import static io.vertx.core.http.HttpHeaders.COOKIE;

/**
 * Created by marco on 19/07/16.
 */
@Builder
public class WebsocketSessionHandler implements Handler<ServerWebSocket> {


    @NonNull
    private final String mountPoint;
    @NonNull
    private final SessionStore sessionStore;
    @NonNull
    private final String cookieName;
    @NonNull
    private final BiConsumer<ServerWebSocket, Session> next;

    @Override
    public void handle(ServerWebSocket serverWebSocket) {
        if (!serverWebSocket.path().startsWith(mountPoint+"/PUSH")) {
            serverWebSocket.reject();
        }
        String cookieHeader = serverWebSocket.headers().get(COOKIE);

        if (cookieHeader != null) {
            Optional<String> sessionId = ServerCookieDecoder.STRICT.decode(cookieHeader).stream()
                .filter(cookie -> cookieName.equals(cookie.name()))
                .findFirst().map(Cookie::value);
            if (sessionId.isPresent()) {
                sessionId.ifPresent(sid -> sessionStore.get(sid, event -> {
                        Session session = null;
                        if (event.succeeded()) {
                            session = event.result();
                        }
                        next.accept(serverWebSocket, session);
                    }
                ));
                return;
            }
        }
        next.accept(serverWebSocket, null);
    }
}
