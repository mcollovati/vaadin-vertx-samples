package com.github.mcollovati.vaadin.exampleapp.samples.push;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.mcollovati.vertx.vaadin.UIProxy;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.vaadin.navigator.View;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import io.vertx.core.Vertx;

public class PushView extends VerticalLayout implements View {

    public static final String VIEW_NAME = "Push";

    private final Label counterLabel = new Label();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger messages = new AtomicInteger();
    private final VerticalLayout messagesLayout;

    public PushView() {
        setHeight(100, Unit.PERCENTAGE);
        Label title = new Label("Push samples");
        title.setStyleName(ValoTheme.LABEL_H1);
        addComponent(title);

        counterLabel.setCaption("Messages pushed from server");
        counterLabel.setValue(Integer.toString(messages.get()));

        addComponent(new HorizontalLayout(
            new Button("Start background thread", this::startBackgroundThread),
            new Button("Start background thread (worker)", this::startBackgroundWithWorker),
            counterLabel
        ));


        messagesLayout = new VerticalLayout();
        messagesLayout.setDefaultComponentAlignment(Alignment.TOP_CENTER);
        Panel panel = new Panel(messagesLayout);
        panel.setSizeFull();
        addComponent(panel);

        setExpandRatio(panel, 1);
        setDefaultComponentAlignment(Alignment.TOP_CENTER);
    }


    private void startBackgroundWithWorker(Button.ClickEvent event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);
        UI ui = getUI();
        new UIProxy(getUI()).runLater(() -> {
            int current = counter.incrementAndGet();
            ui.access(() -> {
                updateCounter();
                messagesLayout.addComponent(
                    new Label(formatStartMessage(sleep, current))
                );
            });
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
            }
            messages.incrementAndGet();
            ui.access(() -> {
                messagesLayout.addComponent(
                    new Label(formatEndMessage(sleep, current))
                );
                updateCounter();
            });

        });
    }

    private void startBackgroundThread(Button.ClickEvent event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);
        Vertx vertx = ((VertxVaadinService) getSession().getService()).getVertx();
        UI ui = getUI();
        vertx.setTimer(1, i -> {
            int current = counter.incrementAndGet();
            ui.access(() -> {
                updateCounter();
                messagesLayout.addComponent(
                    new Label(formatStartMessage(sleep, current))
                );
            });
            vertx.setTimer(sleep * 1000, x -> {
                messages.incrementAndGet();
                ui.access(() -> {
                    messagesLayout.addComponent(
                        new Label(formatEndMessage(sleep, current)));
                    updateCounter();
                });
            });
        });
    }

    private String formatStartMessage(int sleep, int current) {
        String endTime = LocalDateTime.now().atOffset(ZoneOffset.ofTotalSeconds(
            UI.getCurrent().getPage().getWebBrowser().getTimezoneOffset() / 1000
        )).plusSeconds(sleep).toString();
        return String.format(
            "Starting background thread %d, please wait %d seconds until around %s",
            current, sleep, endTime
        );
    }

    private String formatEndMessage(int sleep, int current) {
        String endTime = LocalDateTime.now().atOffset(ZoneOffset.ofTotalSeconds(
            UI.getCurrent().getPage().getWebBrowser().getTimezoneOffset() / 1000
        )).toString();
        return String.format(
            "Background thread %d, completed after %d seconds at %s",
            current, sleep, endTime
        );
    }

    private void updateCounter() {
        counterLabel.setValue(String.format("%d of %d",
            messages.get(), counter.get()
        ));
    }
}
