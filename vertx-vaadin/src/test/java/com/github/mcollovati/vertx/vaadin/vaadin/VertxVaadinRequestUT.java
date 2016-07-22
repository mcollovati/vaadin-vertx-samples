package com.github.mcollovati.vertx.vaadin.vaadin;

import com.github.mcollovati.vertx.vaadin.VertxVaadinRequest;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertx.vaadin.VertxWrappedSession;
import com.github.mcollovati.vertx.vaadin.utils.RandomStringGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by marco on 16/07/16.
 */
@SuppressWarnings("unchecked")
@RunWith(JUnitQuickcheck.class)
public class VertxVaadinRequestUT {

    private static final int TRIALS = 10;
    @Rule
    public MockitoRule mokitoRule = MockitoJUnit.rule();

    @Mock
    HttpServerRequest httpServerRequest;
    @Mock
    RoutingContext routingContext;

    @Mock
    VertxVaadinService vaadinService;

    VertxVaadinRequest vaadinRequest;

    @Before
    public void setUp() {
        when(routingContext.request()).thenReturn(httpServerRequest);
        vaadinRequest = new VertxVaadinRequest(vaadinService, routingContext);
    }


    @Property(trials = TRIALS)
    public void shouldDelegateGetParameterToHttpServerRequest(@From(RandomStringGenerator.class) String paramName,
                                                              @From(RandomStringGenerator.class) String value) {
        when(httpServerRequest.getParam(paramName)).thenReturn(value);
        assertThat(vaadinRequest.getParameter(paramName)).isEqualTo(value);
        assertThat(vaadinRequest.getParameter(paramName + "NotExists")).isNull();
    }

    @Property(trials = TRIALS)
    public void shouldDelegateGetParameterMapToHttpServerRequest(Map<String, List<String>> map) {
        MultiMap multiMap = map.entrySet().stream().collect(MultiMap::caseInsensitiveMultiMap,
            (mm, kv) -> mm.add(kv.getKey(), kv.getValue()), MultiMap::addAll);
        when(httpServerRequest.params()).thenReturn(multiMap);
        Map.Entry<String, String[]>[] expected = map.entrySet().stream()
            .map(e -> entry(e.getKey(),
                e.getValue().stream().toArray(String[]::new)))
            .toArray(Map.Entry[]::new);

        assertThat(vaadinRequest.getParameterMap())
            .containsOnlyKeys(map.keySet().stream().toArray(String[]::new))
            .containsOnly(expected);
    }

    @Property(trials = TRIALS)
    public void shouldDelegateGetContentLengthToHttpServerRequest(@InRange(minInt = 0, maxInt = 5000) int length) {
        when(routingContext.getBody()).thenReturn(Buffer.buffer(new byte[length]));
        assertThat(vaadinRequest.getContentLength()).isEqualTo(length);
    }

    @Property(trials = TRIALS)
    public void shouldDelegateGetInputStreamToHttpServerRequest(String body) throws IOException {
        when(routingContext.getBody()).thenReturn(Buffer.buffer(body));
        assertThat(vaadinRequest.getInputStream())
            .hasSameContentAs(new ByteArrayInputStream(body.getBytes()));
    }

    @Property(trials = TRIALS)
    public void shouldDelegateGetAttributeToRoutingContext(@From(RandomStringGenerator.class) String paramName,
                                                           @From(RandomStringGenerator.class) String value) {
        when(routingContext.get(paramName)).thenReturn(value);
        assertThat(vaadinRequest.getAttribute(paramName)).isEqualTo(value);
        assertThat(vaadinRequest.getAttribute(paramName + "NotExists")).isNull();
    }

    @Property(trials = TRIALS)
    public void shouldDelegateSetAttributeToRoutingContext(@From(RandomStringGenerator.class) String paramName,
                                                           Object value) {
        vaadinRequest.setAttribute(paramName, value);
        verify(routingContext).put(paramName, value);
    }

    @Test
    public void shouldDelegateGetPathInfo() {
        assertPathInfo("", "", "");
        assertPathInfo("", "/", "/");
        assertPathInfo("", "/path", "/path");
        assertPathInfo("", "/path/other", "/path/other");
        assertPathInfo("/ui", "/ui", "");
        assertPathInfo("/ui", "/ui/", "/");
        assertPathInfo("/ui", "/ui/path", "/path");
        assertPathInfo("/ui", "/ui/path/other", "/path/other");
    }

    @Test
    public void shouldDelegateGetContextPath() {
        when(routingContext.mountPoint()).thenReturn(null);
        assertThat(vaadinRequest.getContextPath()).isEqualTo("");
        when(routingContext.mountPoint()).thenReturn("");
        assertThat(vaadinRequest.getContextPath()).isEqualTo("");
        when(routingContext.mountPoint()).thenReturn("/ui");
        assertThat(vaadinRequest.getContextPath()).isEqualTo("/ui");
    }

    @Test
    public void shouldDelegateGetWrappedSessionToRoutingContext() {
        when(routingContext.session()).thenReturn(null);
        assertThat(vaadinRequest.getWrappedSession()).isNull();
        assertThat(vaadinRequest.getWrappedSession(true)).isNull();
        assertThat(vaadinRequest.getWrappedSession(false)).isNull();

        Session session = mock(Session.class);
        when(routingContext.session()).thenReturn(session);
        assertThat(vaadinRequest.getWrappedSession()).isExactlyInstanceOf(VertxWrappedSession.class)
            .extracting(ws -> ((VertxWrappedSession) ws).getVertxSession()).containsExactly(session);
        assertThat(vaadinRequest.getWrappedSession(true)).isExactlyInstanceOf(VertxWrappedSession.class)
            .extracting(ws -> ((VertxWrappedSession) ws).getVertxSession()).containsExactly(session);
        assertThat(vaadinRequest.getWrappedSession(false)).isExactlyInstanceOf(VertxWrappedSession.class)
            .extracting(ws -> ((VertxWrappedSession) ws).getVertxSession()).containsExactly(session);

    }

    @Test
    // TODO: always return null?
    public void shouldDelegateGetContentType() {
        assertThat(vaadinRequest.getContentType()).isNull();
    }

    @Test
    public void shouldDelegateGetLocale() {
        when(routingContext.preferredLocale()).thenReturn(null)
            .thenReturn(Locale.create("en", "us"));
        assertThat(vaadinRequest.getLocale()).isNull();
        assertThat(vaadinRequest.getLocale()).isEqualTo(java.util.Locale.forLanguageTag("en-US"));
    }

    @Test
    public void shouldDelegateGetRemoteAddress() {
        when(httpServerRequest.remoteAddress())
            .thenReturn(null)
            .thenReturn(new SocketAddressImpl(8080, "10.3.100.108"));
        assertThat(vaadinRequest.getRemoteAddr()).isNull();
        assertThat(vaadinRequest.getRemoteAddr()).isEqualTo("10.3.100.108");
    }

    @Test
    public void shouldDelegateIsSecure() {
        when(httpServerRequest.isSSL())
            .thenReturn(false).thenReturn(true);
        assertThat(vaadinRequest.isSecure()).isFalse();
        assertThat(vaadinRequest.isSecure()).isTrue();
    }

    @Property(trials = TRIALS)
    public void shouldDelegateGetHeader(@From(RandomStringGenerator.class) String name,
                                        @From(RandomStringGenerator.class) String value) {
        when(httpServerRequest.getHeader(name)).thenReturn(value);
        assertThat(vaadinRequest.getHeader(name)).isEqualTo(value);
        assertThat(vaadinRequest.getHeader(name+"notExist")).isNull();
    }

    @Test
    public void shouldDelegateGetCookies() {
        Cookie cookie1 = Cookie.cookie("cookie1", "value1")
            .setDomain("domain").setHttpOnly(true)
            .setMaxAge(90L).setPath("path").setSecure(true);
        Cookie cookie2 = Cookie.cookie("cookie2", "value2");
        when(routingContext.cookieCount()).thenReturn(0).thenReturn(2);
        when(routingContext.cookies()).thenReturn(new LinkedHashSet<>(Arrays.asList(cookie1, cookie2)));
        assertThat(vaadinRequest.getCookies()).isNull();
        javax.servlet.http.Cookie[] cookies = vaadinRequest.getCookies();
        assertThat(cookies).hasSize(2);
        assertThat(cookies[0]).extracting("name", "value", "domain", "httpOnly", "maxAge", "path", "secure")
            .containsExactly(cookie1.getName(), cookie1.getValue(), cookie1.getDomain(), true, 90, "path", true);
        assertThat(cookies[1]).extracting("name", "value", "domain", "httpOnly", "maxAge", "path", "secure")
            .containsExactly(cookie2.getName(), cookie2.getValue(), null, false, -1, null, false);
    }

    @Test
    @Ignore
    public void shouldDelegateGetAuthType()  {

    }

    @Test
    public void shouldDelegateGetRemoteUser()  {
        User user = mock(User.class);
        when(routingContext.user()).thenReturn(null);

        assertThat(vaadinRequest.getRemoteUser()).isNull();

    }

    /*
Method              URL-Decoded Result
-------------------------------------------------
getContextPath()                /app
getLocalAddr()                  127.0.0.1
getLocalName()                  30thh.loc
getLocalPort()                  8480
getMethod()                     GET
getPathInfo()           yes     /a?+b
getProtocol()                   HTTP/1.1
getQueryString()        no      p+1=c+d&p+2=e+f
getRequestedSessionId() no      S%3F+ID
getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
getScheme()                     http
getServerName()                 30thh.loc
getServerPort()                 8480
getServletPath()        yes     /test?
getParameterNames()     yes     [p 2, p 1]
getParameter("p 1")     yes     c d
     */


    private void assertPathInfo(String mountPoint, String path, String expected) {
        when(httpServerRequest.path()).thenReturn(path);
        when(routingContext.mountPoint()).thenReturn(mountPoint);
        assertThat(vaadinRequest.getPathInfo()).isEqualTo(expected);
    }

}
