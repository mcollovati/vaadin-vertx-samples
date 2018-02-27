package com.github.mcollovati.vaadin.exampleapp.samples.push;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.mcollovati.vertx.vaadin.UIProxy;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.vaadin.navigator.View;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
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
        addComponent(counterLabel);

        Button button = new Button("Start background thread", this::startBackgroundThread);
        addComponent(button);

        messagesLayout = new VerticalLayout();
        messagesLayout.setDefaultComponentAlignment(Alignment.TOP_CENTER);
        Panel panel = new Panel(messagesLayout);
        panel.setSizeFull();
        addComponent(panel);

        setExpandRatio(panel, 1);
        setDefaultComponentAlignment(Alignment.TOP_CENTER);
    }


    private void startBackgroundThread2(Button.ClickEvent event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);
        Vertx vertx = ((VertxVaadinService) getSession().getService()).getVertx();

        new UIProxy(getUI()).runLater2(() -> {
            int current = counter.incrementAndGet();
            getUI().access(() -> {
                addComponent(new Label("Starting background thread " + current + ", please wait " + sleep + " seconds"));
            });
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
            }
            getUI().access(() -> {
                addComponent(new Label("background thread " + current + "completed after " + sleep + " seconds"));
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
                messagesLayout.addComponent(
                    new Label("Starting background thread " + current + ", please wait " + sleep + " seconds")
                );
            });
            vertx.setTimer(sleep * 1000, x -> {
                ui.access(() -> {
                    messagesLayout.addComponent(
                        new Label("background thread " + current + "completed after " + sleep + " seconds"));
                    updateCounter();
                });
            });
        });
    }

    private void updateCounter() {
        counterLabel.setValue(Integer.toString(messages.incrementAndGet()));
    }
}
