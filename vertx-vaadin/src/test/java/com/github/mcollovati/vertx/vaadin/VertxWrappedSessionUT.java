package com.github.mcollovati.vertx.vaadin;

import io.vertx.ext.web.Session;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.atmosphere.vertx.VertxHttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by marco on 25/07/16.
 */
public class VertxWrappedSessionUT {

    @Rule
    public MockitoRule mokitoRule = MockitoJUnit.rule();

    @Mock
    Session session;

    @Mock
    HttpSessionBindingListener sessionBindingListenerObject;

    VertxWrappedSession vertxWrappedSession;

    @Before
    public void setUp() throws Exception {
        vertxWrappedSession = new VertxWrappedSession(session);
    }

    @Test
    public void getMaxInactiveInterval() throws Exception {

    }

    @Test
    public void setMaxInactiveInterval() throws Exception {

    }

    @Test
    public void shouldDelegateGetAttribute() throws Exception {
        String attrName = "attributeName";
        Object value = new Object();
        when(session.get(attrName)).thenReturn(value, null);
        when(session.isDestroyed()).thenReturn(false);
        assertThat(vertxWrappedSession.getAttribute(attrName)).isSameAs(value);
        assertThat(vertxWrappedSession.getAttribute(attrName)).isNull();
    }

    @Test
    public void getAttributeShouldThrowExceptionWhenSessionIsInvalidated() {
        shouldThrowExceptionWhenSessionIsInvalidated(() -> vertxWrappedSession.getAttribute("attr"));
    }

    @Test
    public void shouldDelegateSetAttribute() throws Exception {
        String attrName = "attributeName";
        Object value1 = new Object();
        vertxWrappedSession.setAttribute(attrName, value1);
        verify(session).put(attrName, value1);
        vertxWrappedSession.setAttribute(attrName, null);
        verify(session).remove(attrName);
    }

    @Test
    public void setAttributeShuoldInvokeValueBoundForHttpSessionBindingListener() throws Exception {
        String attrName = "attributeName";
        vertxWrappedSession.setAttribute(attrName, sessionBindingListenerObject);
        ArgumentCaptor<HttpSessionBindingEvent> sessionBindingEventCaptor = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(sessionBindingListenerObject).valueBound(sessionBindingEventCaptor.capture());
        assertHttpSessionBindingEvent(attrName, sessionBindingEventCaptor.getValue());
    }

    @Test
    public void setAttributeShuoldInvokeValueUnboundForReplacedHttpSessionBindingListener() throws Exception {
        String attrName = "attributeName";
        when(session.get(attrName)).thenReturn(sessionBindingListenerObject);
        vertxWrappedSession.setAttribute(attrName, new Object());
        ArgumentCaptor<HttpSessionBindingEvent> sessionBindingEventCaptor = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(sessionBindingListenerObject).valueUnbound(sessionBindingEventCaptor.capture());
        assertHttpSessionBindingEvent(attrName, sessionBindingEventCaptor.getValue());
    }

    private void assertHttpSessionBindingEvent(String attrName, HttpSessionBindingEvent sessionBindingEvent) {
        assertHttpSessionBindingEvent(attrName, sessionBindingListenerObject, sessionBindingEvent);
    }
    private void assertHttpSessionBindingEvent(String attrName, Object value, HttpSessionBindingEvent sessionBindingEvent) {
        assertThat(sessionBindingEvent.getSession())
            .isInstanceOf(VertxHttpSession.class)
            .extracting("delegate").contains(vertxWrappedSession);
        assertThat(sessionBindingEvent.getName()).isEqualTo(attrName);
        assertThat(sessionBindingEvent.getValue()).isEqualTo(value);
    }

    // TODO: test javax.servlet.http.HttpSessionAttributeListener

    @Test
    public void setAttributeShouldThrowExceptionWhenSessionIsInvalidated() {
        shouldThrowExceptionWhenSessionIsInvalidated(() -> vertxWrappedSession.setAttribute("attr", new Object()));
    }

    @Test
    public void shouldDelegateGetAttributeNames() throws Exception {
        Map<String, Object> sampleData = new HashMap<>();
        sampleData.put("a", "a");
        sampleData.put("b", "b");
        sampleData.put("c", "c");
        when(session.data()).thenReturn(emptyMap(), sampleData);
        assertThat(vertxWrappedSession.getAttributeNames()).isEmpty();
        assertThat(vertxWrappedSession.getAttributeNames()).containsOnly("a", "b", "c");
    }

    @Test
    public void getAttributeNamesShouldThrowExceptionWhenSessionIsInvalidated() {
        shouldThrowExceptionWhenSessionIsInvalidated(() -> vertxWrappedSession.getAttributeNames());
    }

    @Test
    public void shouldDelegateInvalidate() throws Exception {
        vertxWrappedSession.invalidate();
        verify(session).destroy();
    }
    @Test
    public void shouldUnbindOnInvalidate() throws Exception {

        Map<String, Object> sampleData = new HashMap<>();
        HttpSessionBindingListener mockA = mock(HttpSessionBindingListener.class);
        HttpSessionBindingListener mockC = mock(HttpSessionBindingListener.class);
        sampleData.put("a", mockA);
        sampleData.put("b", "b");
        sampleData.put("c", mockC);
        sampleData.put("b", "b");
        when(session.data()).thenReturn(sampleData);
        vertxWrappedSession.invalidate();
        verify(session).destroy();
        ArgumentCaptor<HttpSessionBindingEvent> sessionBindingEventCaptor = ArgumentCaptor.forClass(HttpSessionBindingEvent.class);
        verify(mockA).valueUnbound(sessionBindingEventCaptor.capture());
        verify(mockC).valueUnbound(sessionBindingEventCaptor.capture());
        assertThat(sessionBindingEventCaptor.getAllValues()).hasSize(2);
        assertHttpSessionBindingEvent("a", mockA, sessionBindingEventCaptor.getAllValues().get(0));
        assertHttpSessionBindingEvent("c", mockC, sessionBindingEventCaptor.getAllValues().get(1));
    }

    @Test
    public void invalidatShouldThrowExceptionWhenSessionIsInvalidated() {
        shouldThrowExceptionWhenSessionIsInvalidated(() -> vertxWrappedSession.invalidate());
    }


    @Test
    public void getId() throws Exception {

    }

    @Test
    public void getCreationTime() throws Exception {

    }

    @Test
    public void getLastAccessedTime() throws Exception {

    }

    @Test
    public void isNew() throws Exception {

    }

    @Test
    public void removeAttribute() throws Exception {

    }


    private void shouldThrowExceptionWhenSessionIsInvalidated(ThrowableAssert.ThrowingCallable op) {
        when(session.isDestroyed()).thenReturn(true);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(op);
    }

}