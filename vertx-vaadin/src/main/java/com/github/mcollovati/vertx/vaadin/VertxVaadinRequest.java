package com.github.mcollovati.vertx.vaadin;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WrappedSession;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.CookieImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.Cookie;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by marco on 16/07/16.
 */
public class VertxVaadinRequest implements VaadinRequest {

    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("^(.*/[^;]+)(?:;.*$|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARSET_PATTERN = Pattern.compile("^.*(?<=charset=)([^;]+)(?:;.*$|$)", Pattern.CASE_INSENSITIVE);

    private final VertxVaadinService service;
    private final RoutingContext routingContext;
    private final HttpServerRequest request;

    public VertxVaadinRequest(VertxVaadinService service, RoutingContext routingContext) {
        this.service = service;
        this.routingContext = routingContext;
        this.request = routingContext.request();

    }

    public HttpServerRequest getRequest() {
        return request;
    }

    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    @Override
    public String getParameter(String parameter) {
        return request.getParam(parameter);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return request.params().names()
            .stream().collect(toMap(identity(), k -> request.params().getAll(k).stream().toArray(String[]::new)));
    }

    @Override
    public int getContentLength() {
        return routingContext.getBody().length();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(routingContext.getBody().getBytes());
    }

    @Override
    public Object getAttribute(String name) {
        return routingContext.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        routingContext.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        routingContext.put(name, null);
    }

    @Override
    public String getPathInfo() {
        return request.path().substring(getContextPath().length());
    }

    @Override
    public String getContextPath() {
        return Optional.ofNullable(routingContext.mountPoint()).orElse("");
    }

    @Override
    public WrappedSession getWrappedSession() {
        return getWrappedSession(true);
    }

    @Override
    public WrappedSession getWrappedSession(boolean allowSessionCreation) {
        return Optional.ofNullable(routingContext.session())
            .map(VertxWrappedSession::new).orElse(null);
    }

    @Override
    public String getContentType() {
        return Optional.ofNullable(request.getHeader(HttpHeaders.CONTENT_TYPE))
            .map(CONTENT_TYPE_PATTERN::matcher).filter(Matcher::matches)
            .map(m -> m.group(1)).orElse(null);
    }

    @Override
    public Locale getLocale() {
        // TODO: in utility class
        io.vertx.ext.web.Locale loc = routingContext.preferredLocale();
        return toJavaLocale(loc);
    }

    @Override
    public String getRemoteAddr() {
        return Optional.ofNullable(request.remoteAddress())
            .map(SocketAddress::host).orElse(null);
    }

    @Override
    public boolean isSecure() {
        return request.isSSL();
    }

    @Override
    public String getHeader(String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    public VertxVaadinService getService() {
        return service;
    }

    @Override
    public Cookie[] getCookies() {
        if (routingContext.cookieCount() > 0) {
            return routingContext.cookies().stream().map(this::mapCookie).toArray(Cookie[]::new);
        }
        return null;
    }

    private Cookie mapCookie(io.vertx.ext.web.Cookie cookie) {
        io.netty.handler.codec.http.cookie.Cookie decoded = ClientCookieDecoder.STRICT.decode(cookie.encode());
        Cookie out = new Cookie(decoded.name(), decoded.value());
        Optional.ofNullable(decoded.domain()).ifPresent(out::setDomain);
        out.setPath(decoded.path());
        out.setHttpOnly(decoded.isHttpOnly());
        out.setSecure(decoded.isSecure());
        if (decoded.maxAge() != Long.MIN_VALUE) {
            out.setMaxAge((int) decoded.maxAge());
        }

        // TODO extract other values
        return out;
    }

    private void mapCookieImpl(CookieImpl vertxCookie, Cookie servletCookie) {

    }

    // TODO
    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return Optional.ofNullable(routingContext.user())
            .map(User::principal).flatMap(json -> Optional.ofNullable(json.getString("username")))
            .orElse(null);
    }

    @Override
    public Principal getUserPrincipal() {
        return Optional.ofNullable(routingContext.user())
            .map(VertxPrincipal::new)
            .orElse(null);
    }

    // TODO
    @Override
    public boolean isUserInRole(String role) {
        if (routingContext.user() != null) {
            Future<Boolean> userInRole = Future.future();
            return Sync.await(completer -> routingContext.user().isAuthorised(role, completer));
        }
        return false;
    }


    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(routingContext.data().keySet());
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(routingContext.acceptableLocales().stream()
            .map(VertxVaadinRequest::toJavaLocale).collect(toList()));
    }

    @Override
    public String getRemoteHost() {
        return request.host();
    }

    @Override
    public int getRemotePort() {
        return Optional.ofNullable(request.remoteAddress())
            .map(SocketAddress::port).orElse(-1);
    }

    @Override
    public String getCharacterEncoding() {
        return Optional.ofNullable(request.getHeader(HttpHeaders.CONTENT_TYPE))
            .map(CHARSET_PATTERN::matcher).filter(Matcher::matches)
            .map(m -> m.group(1)).orElse(null);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(routingContext.getBodyAsString()));
    }

    @Override
    public String getMethod() {
        return request.rawMethod();
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(request.headers().getAll(name));
    }

    private static Locale toJavaLocale(io.vertx.ext.web.Locale locale) {
        return Optional.ofNullable(locale)
            .map(loc -> new Locale(loc.language(), loc.country(), loc.variant()))
            .orElse(null);
    }

    public static Optional<VertxVaadinRequest> tryCast(VaadinRequest request) {
        if (request instanceof VertxVaadinRequest) {
            return Optional.of((VertxVaadinRequest) request);
        }
        return Optional.empty();
    }


    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class VertxPrincipal implements Principal {

        private final User user;

        @Override
        public String getName() {
            return user.principal().getString("username");
        }
    }
}
