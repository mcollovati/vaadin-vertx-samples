package com.github.mcollovati.vertx.vaadin.sample;

import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertx.vaadin.communication.SockJSPushConnection;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import io.vertx.core.Vertx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by marco on 16/07/16.
 */
@Theme("valo")
@Title("Vert.x vaadin push sample")
@Widgetset("com.github.mcollovati.vertx.vaadin.VaadinVertxWidgetset")
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class PushTestUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        setPushConnection(new SockJSPushConnection(this));
        String deploymentId = request.getService().getServiceName();

        Label time = new Label();
        AtomicLong timerId = new AtomicLong(-1);
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.addComponent(new Label("OK!!!! " + getLocale()));
        verticalLayout.addComponent(new Label("Vertx!!!! " + deploymentId ));
        verticalLayout.addComponent(new Label("Session ID!!!! " + request.getWrappedSession().getId() ));
        verticalLayout.addComponent(time);
        verticalLayout.addComponent(new Button("CLICK", e -> {

            if (timerId.get() == -1) {
                timerId.set(
                    getVertx().setPeriodic(1000, event -> {
                        access(() -> showNow(time, getLocale()));
                    }));
            } else {
                getVertx().cancelTimer(timerId.getAndSet(-1));
                access(() -> time.setValue("STOP"));
            }

        }));
        showNow(time, getLocale());
        setContent(verticalLayout);
    }

    private Vertx getVertx() {
        return ((VertxVaadinService)getSession().getService()).getVertx();
    }


    private static void showNow(Label label, Locale locale) {
        System.out.println("================ Show time " + LocalDateTime.now());
        label.setValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", locale)));
    }


    //@VaadinServletConfiguration(widgetset = "pippo")
    //@VaadinVerticleConfiguration(mountPoint="/uitest")
    public static class MyVerticle extends VaadinVerticle {}
}
