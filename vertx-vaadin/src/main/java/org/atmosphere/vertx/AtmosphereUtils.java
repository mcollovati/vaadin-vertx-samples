/*
 * Copyright 2015 Async-IO.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.vertx;

import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

public class AtmosphereUtils {

    private static Logger logger = LoggerFactory.getLogger(AtmosphereUtils.class);

    public final static AtmosphereRequest request(final HttpServerRequest request) throws Throwable {
        return request(request, null);
    }

    public final static AtmosphereRequest request(final HttpServerRequest request,
                                                  final Consumer<AtmosphereRequest.Builder> configurer) throws Throwable {
        final String base = getBaseUri(request);
        final URI requestUri = new URI(base.substring(0, base.length() - 1) + request.uri());
        String ct = "text/plain";
        if (request.headers().get("Content-Type") != null) {
            ct = request.headers().get("Content-Type");
        }
        String method = request.method().name();


        URI uri = null;
        try {
            uri = URI.create(request.uri());
        } catch (IllegalArgumentException e) {
            logger.trace("", e);
        }
        String queryString = uri.getQuery();
        Map<String, String[]> qs = new HashMap<String, String[]>();
        if (queryString != null) {
            parseQueryString(qs, queryString);
        }

        String u = requestUri.toURL().toString();
        int last = u.indexOf("?") == -1 ? u.length() : u.indexOf("?");
        String url = u.substring(0, last);
        int l = requestUri.getAuthority().length() + requestUri.getScheme().length() + 3;

        final Map<String, Object> attributes = new HashMap<String, Object>();

        final StringBuilder b = new StringBuilder();

        int port = uri == null ? 0 : uri.getPort();
        String uriString = uri.getPath();
        String host = uri.getHost();
        AtmosphereRequest.Builder requestBuilder = new AtmosphereRequestImpl.Builder()
            .requestURI(url.substring(l))
            .requestURL(u)
            .pathInfo(url.substring(l))
            .headers(getHeaders(request))
            .method(method)
            .requestURL(request.uri())
            .contentType(ct)
            .destroyable(false)
            .attributes(attributes)
            .servletPath("")
            .remotePort(port)
            .remoteAddr(uriString)
            .remoteHost(host)
//                .localPort(((InetSocketAddress) ctx.getChannel().getLocalAddress()).getPort())
//                .localAddr(((InetSocketAddress) ctx.getChannel().getLocalAddress()).getAddress().getHostAddress())
//                .localName(((InetSocketAddress) ctx.getChannel().getLocalAddress()).getHostName())
            .body(b.toString())
            .queryStrings(qs);
        Optional.of(configurer).ifPresent(c -> c.accept(requestBuilder));
        final AtmosphereRequest r = requestBuilder.build();
        return r;
    }


    public static void parseQueryString(Map<String, String[]> qs, String queryString) {
        if (queryString != null) {
            String[] s = queryString.split("&");
            for (String a : s) {
                String[] q = a.split("=");
                String[] z = new String[]{q.length > 1 ? q[1] : ""};
                qs.put(q[0], z);
            }
        }
    }

    public static String getBaseUri(final HttpServerRequest request) {
        return "http://" + request.headers().get(HttpHeaders.Names.HOST) + "/";

    }

    public static Map<String, String> getHeaders(final HttpServerRequest request) {
        final Map<String, String> headers = new HashMap<String, String>();

        for (Entry<String, String> e : request.headers()) {
            headers.put(e.getKey().toLowerCase(), e.getValue());
        }

        return headers;
    }

    public static HttpServerRequest requestfromAtmosphere(AtmosphereRequest atmosphereRequest) {
        final URI uri = URI.create(atmosphereRequest.getRequestURI());
        return new HttpServerRequest() {
            @Override
            public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
                return this;
            }

            @Override
            public HttpServerRequest handler(Handler<Buffer> handler) {
                return this;
            }

            @Override
            public HttpServerRequest pause() {
                return this;
            }

            @Override
            public HttpServerRequest resume() {
                return this;
            }

            @Override
            public HttpServerRequest endHandler(Handler<Void> endHandler) {
                return this;
            }

            @Override
            public HttpVersion version() {
                return HttpVersion.HTTP_1_1;
            }

            @Override
            public HttpMethod method() {
                return HttpMethod.valueOf(atmosphereRequest.getMethod());
            }

            @Override
            public String rawMethod() {
                return atmosphereRequest.getMethod();
            }

            @Override
            public boolean isSSL() {
                return atmosphereRequest.isSecure();
            }

            @Override
            public String scheme() {
                return atmosphereRequest.getScheme();
            }

            @Override
            public String uri() {
                return atmosphereRequest.getRequestURI();
            }

            @Override
            public String path() {
                return atmosphereRequest.getPathInfo();
            }

            @Override
            public String query() {
                return atmosphereRequest.getQueryString();
            }

            @Override
            public String host() {
                return atmosphereRequest.getRemoteHost();
            }

            @Override
            public HttpServerResponse response() {
                return null;
            }

            @Override
            public MultiMap headers() {
                return MultiMap.caseInsensitiveMultiMap().addAll(atmosphereRequest.headersMap());
            }

            @Override
            public String getHeader(String headerName) {
                return atmosphereRequest.getHeader(headerName, false);
            }

            @Override
            public String getHeader(CharSequence headerName) {
                return getHeader(headerName.toString());
            }

            @Override
            public MultiMap params() {
                return atmosphereRequest.getParameterMap()
                    .entrySet().stream()
                    .collect(MultiMap::caseInsensitiveMultiMap,
                        (mm, e) -> mm.add(e.getKey(), Arrays.<String>asList(e.getValue())),
                        MultiMap::addAll);

            }

            @Override
            public String getParam(String paramName) {
                return atmosphereRequest.getParameter(paramName);
            }

            @Override
            public SocketAddress remoteAddress() {
                return new SocketAddressImpl(uri.getPort(), uri.getHost());
            }

            @Override
            public SocketAddress localAddress() {
                URI localAddr = URI.create(atmosphereRequest.getLocalAddr());
                return new SocketAddressImpl(localAddr.getPort(), localAddr.getHost());
            }

            @Override
            public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
                return new X509Certificate[0];
            }

            @Override
            public String absoluteURI() {
                return uri.toString();
            }

            @Override
            public NetSocket netSocket() {
                return null;
            }

            @Override
            public HttpServerRequest setExpectMultipart(boolean expect) {
                return this;
            }

            @Override
            public boolean isExpectMultipart() {
                return false;
            }

            @Override
            public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
                return this;
            }

            @Override
            public MultiMap formAttributes() {
                return params();
            }

            @Override
            public String getFormAttribute(String attributeName) {
                return getParam(attributeName);
            }

            @Override
            public ServerWebSocket upgrade() {
                return null;
            }

            @Override
            public boolean isEnded() {
                return true;
            }

            @Override
            public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
                return this;
            }

            @Override
            public HttpConnection connection() {
                return null;
            }
        };
    }

}
