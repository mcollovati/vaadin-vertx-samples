package com.github.mcollovati.vertxvaadin.flowdemo.push;


import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.mcollovati.vertx.vaadin.UIProxy;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertxvaadin.flowdemo.MainLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import io.vertx.core.Vertx;

@Route(value = "Push", layout = MainLayout.class)
public class PushView extends VerticalLayout  {

    public static final String VIEW_NAME = "Push";

    private final Label counterLabel = new Label();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger messages = new AtomicInteger();
    private final VerticalLayout messagesLayout;

    public PushView() {
        setHeight("100%");
        H1 title = new H1("Push samples");
        add(title);

        counterLabel.setTitle("Messages pushed from server");
        counterLabel.setText(Integer.toString(messages.get()));

        add(new HorizontalLayout(
            new Button("Start background thread", this::startBackgroundThread),
            new Button("Start background thread (worker)", this::startBackgroundWithWorker),
            counterLabel
        ));


        messagesLayout = new VerticalLayout();
        messagesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        Div panel = new Div(messagesLayout);
        panel.setSizeFull();
        add(panel);

        setFlexGrow(1.0, panel);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }


    private void startBackgroundWithWorker(ClickEvent<Button> event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);
        UI ui = getUI().orElse(null);
        if (ui != null) {
            new UIProxy(ui).runLater(() -> {
                int current = counter.incrementAndGet();
                ui.access(() -> {
                    updateCounter();
                    messagesLayout.add(
                        new Label(formatStartMessage(ui, sleep, current))
                    );
                });
                try {
                    Thread.sleep(sleep * 1000);
                } catch (InterruptedException e) {
                }
                messages.incrementAndGet();
                ui.access(() -> {
                    messagesLayout.add(
                        new Label(formatEndMessage(ui, sleep, current))
                    );
                    updateCounter();
                });

            });
        }
    }

    private void startBackgroundThread(ClickEvent<Button> event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);

        Vertx vertx = ((VertxVaadinService) VaadinService.getCurrent()).getVertx();
        UI ui = getUI().orElse(null);
        if (ui != null) {
            vertx.setTimer(1, i -> {
                int current = counter.incrementAndGet();
                ui.access(() -> {
                    updateCounter();
                    messagesLayout.add(
                        new Label(formatStartMessage(ui, sleep, current))
                    );
                });
                vertx.setTimer(sleep * 1000, x -> {
                    messages.incrementAndGet();
                    ui.access(() -> {
                        messagesLayout.add(
                            new Label(formatEndMessage(ui, sleep, current)));
                        updateCounter();
                    });
                });
            });
        }
    }

    private String formatStartMessage(UI ui, int sleep, int current) {
        String endTime = LocalDateTime.now().atOffset(ZoneOffset.ofTotalSeconds(
            ui.getSession().getBrowser().getTimezoneOffset() / 1000
        )).plusSeconds(sleep).toString();
        return String.format(
            "Starting background thread %d, please wait %d seconds until around %s",
            current, sleep, endTime
        );
    }

    private String formatEndMessage(UI ui, int sleep, int current) {
        String endTime = LocalDateTime.now().atOffset(ZoneOffset.ofTotalSeconds(

            ui.getSession().getBrowser().getTimezoneOffset() / 1000
        )).toString();
        return String.format(
            "Background thread %d, completed after %d seconds at %s",
            current, sleep, endTime
        );
    }

    private void updateCounter() {
        counterLabel.setText(String.format("%d of %d",
            messages.get(), counter.get()
        ));
    }
}
