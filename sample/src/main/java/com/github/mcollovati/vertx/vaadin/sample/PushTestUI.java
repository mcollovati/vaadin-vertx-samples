package com.github.mcollovati.vertx.vaadin.sample;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertx.vaadin.communication.SockJSPushConnection;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.HasValue;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import io.vertx.core.Vertx;

/**
 * Created by marco on 16/07/16.
 */
@Theme("valo")
@Title("Vert.x vaadin push sample")
@Widgetset("com.github.mcollovati.vertx.vaadin.VaadinVertxWidgetset")
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class PushTestUI extends UI {

    private AtomicLong timerId;

    @Override
    protected void init(VaadinRequest request) {
        setPushConnection(new SockJSPushConnection(this));
        String deploymentId = request.getService().getServiceName();


        ComboBox<Transport> transport = new ComboBox<>("PUSH Transport");
        transport.setItems(Transport.values());
        transport.setItemCaptionGenerator(Transport::name);
        transport.setValue(getPushConfiguration().getTransport());
        transport.addValueChangeListener(e -> {
            transportValueChangeListener(getPushConfiguration()::setTransport).valueChange(e);
        });

        ComboBox<Transport> fallbackTransport = new ComboBox<>("Fallback PUSH Transport");
        fallbackTransport.setItems(Transport.values());
        fallbackTransport.setItemCaptionGenerator(Transport::name);
        fallbackTransport.setValue(getPushConfiguration().getFallbackTransport());
        fallbackTransport.addValueChangeListener(transportValueChangeListener(getPushConfiguration()::setFallbackTransport));


        Label time = new Label();
        timerId = new AtomicLong(-1);
        VerticalLayout verticalLayout = new VerticalLayout();

        // TODO: find correct way to disable and then reenable PUSH to change transport
        //verticalLayout.addComponent(new HorizontalLayout(transport, fallbackTransport));

        verticalLayout.addComponent(new Label("OK!!!! " + getLocale()));
        verticalLayout.addComponent(new Label("Vertx!!!! " + deploymentId));
        verticalLayout.addComponent(new Label("Session ID!!!! " + request.getWrappedSession().getId()));
        verticalLayout.addComponent(time);
        verticalLayout.addComponent(new Button("Start PUSH clock", e -> {
            e.getButton().setCaption("Stop PUSH clock");
            if (timerId.get() == -1) {
                timerId.set(
                    getVertx().setPeriodic(1000, event -> {
                        access(() -> showNow(time, getLocale()));
                    }));
            } else {
                cancelTimer();
                access(() -> {
                    time.setValue("STOP");
                    e.getButton().setCaption("Restart PUSH clock");
                });
            }

        }));
        showNow(time, getLocale());
        setContent(verticalLayout);

    }

    @Override
    public void detach() {
        cancelTimer();
        super.detach();
    }

    private void cancelTimer() {
        long currentTimer = timerId.getAndSet(-1);
        if (currentTimer > 0) {
            getVertx().cancelTimer(currentTimer);
        }
    }

    private HasValue.ValueChangeListener<Transport> transportValueChangeListener(Consumer<Transport> setter) {
        return event -> {
            Optional.ofNullable(event.getValue()).ifPresent(setter);
        };
    }

    private Vertx getVertx() {
        return ((VertxVaadinService) getSession().getService()).getVertx();
    }


    private static void showNow(Label label, Locale locale) {
        System.out.println("================ Show time " + LocalDateTime.now());
        label.setValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", locale)));
    }


    //@VaadinServletConfiguration(widgetset = "pippo")
    //@VaadinVerticleConfiguration(mountPoint="/uitest")
    public static class MyVerticle extends VaadinVerticle {}
}
