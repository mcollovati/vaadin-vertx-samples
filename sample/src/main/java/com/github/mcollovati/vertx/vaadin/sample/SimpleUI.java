package com.github.mcollovati.vertx.vaadin.sample;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;
import java.time.Instant;

import com.github.mcollovati.vertx.vaadin.VertxVaadinRequest;
import com.github.mcollovati.vertx.vaadin.communication.SockJSPushConnection;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.viritin.label.Header;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 * Created by marco on 21/07/16.
 */
@Theme(ValoTheme.THEME_NAME)
@Title("Vert.x vaadin sample")
//@Widgetset("com.github.mcollovati.vertx.vaadin.VaadinVertxWidgetset")
@Widgetset("com.vaadin.DefaultWidgetSet")
public class SimpleUI extends UI {

    @Override
    protected void init(VaadinRequest request) {

        VertxVaadinRequest req = (VertxVaadinRequest) request;
        Cookie cookie = new Cookie("myCookie", "myValue");
        cookie.setMaxAge(120);
        cookie.setPath(req.getContextPath());
        VaadinService.getCurrentResponse().addCookie(cookie);

        Label sessionAttributeLabel = new MLabel().withCaption("Session listener");


        String deploymentId = req.getService().getVertx().getOrCreateContext().deploymentID();

        setContent(new MVerticalLayout(
            new Header("Vert.x Vaadin Sample").setHeaderLevel(1),
            new Label(String.format("Verticle %s deployed on Vert.x", deploymentId)),
            new Label("Session created at " + Instant.ofEpochMilli(req.getWrappedSession().getCreationTime())),
            sessionAttributeLabel
        ).withFullWidth());
    }

    @Override
    public void attach() {
        super.attach();
        //HttpSessionBindingListener sessionAttr = new SampleHttpSessionBindingListener();
        //getSession().getSession().setAttribute("myAttribute", sessionAttr);
    }


    private class SampleHttpSessionBindingListener implements HttpSessionBindingListener, Serializable {
        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            System.out.println("================================================== bound " + event.getName());
            getUI().access(() -> Notification.show("Attribute Set"));

        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            System.out.println("================================================== unbound " + event.getName());
            getUI().access(() -> Notification.show("Attribute removed"));
        }
    }
}
