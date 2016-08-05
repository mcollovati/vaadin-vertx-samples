package com.github.mcollovati.vertx.vaadin.sample;

import com.github.mcollovati.vertx.vaadin.VertxVaadinRequest;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.viritin.label.Header;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.time.Instant;

/**
 * Created by marco on 21/07/16.
 */
@Theme(ValoTheme.THEME_NAME)
@Title("Vert.x vaadin sample")
@Push
public class SimpleUI extends UI {
    @Override
    protected void init(VaadinRequest request) {

        VertxVaadinRequest req = (VertxVaadinRequest) request;
        Cookie cookie = new Cookie("myCookie", "myValue");
        cookie.setMaxAge(120);
        cookie.setPath(req.getContextPath());
        VaadinService.getCurrentResponse().addCookie(cookie);

        Label sessionAttributeLabel = new MLabel().withCaption("Session listener");



        setContent(new MVerticalLayout(
            new Header("Vert.x Vaadin Sample").setHeaderLevel(1),
            new Label(String.format("Verticle %s deployed on Vert.x",
                req.getService().getVerticle().deploymentID())),
            new Label("Session created at " + Instant.ofEpochMilli(req.getWrappedSession().getCreationTime())),
            sessionAttributeLabel
        ).withFullWidth());
    }

    @Override
    public void attach() {
        super.attach();
        HttpSessionBindingListener sessionAttr = new HttpSessionBindingListener() {
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
        };
        getSession().getSession().setAttribute("myAttribute", sessionAttr);

    }
}
