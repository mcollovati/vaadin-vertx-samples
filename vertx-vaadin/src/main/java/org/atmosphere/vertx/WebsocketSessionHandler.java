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
        String basePath = Optional.ofNullable(mountPoint)
            .map(m -> m.substring(0, m.lastIndexOf('/')) )
            .orElse("");

        if (!serverWebSocket.path().startsWith(basePath + "/PUSH")) {
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
