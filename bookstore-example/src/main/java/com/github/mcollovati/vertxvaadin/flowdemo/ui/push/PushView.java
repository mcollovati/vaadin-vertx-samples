package com.github.mcollovati.vertxvaadin.flowdemo.ui.push;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.mcollovati.vertx.vaadin.UIProxy;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.github.mcollovati.vertxvaadin.flowdemo.ui.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import io.vertx.core.Vertx;

@Route(value = "Push", layout = MainLayout.class)
public class PushView extends VerticalLayout {

    public static final String VIEW_NAME = "Push";

    private final Label counterLabel = new Label();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger messages = new AtomicInteger();
    private final VerticalLayout messagesLayout;
    private final CopyOnWriteArrayList<ProgressBar> bars = new CopyOnWriteArrayList<>();
    private transient Registration taskTimer;

    public PushView() {
        setHeight("100%");
        H1 title = new H1("Push samples");
        add(title);

        counterLabel.setTitle("Messages pushed from server");
        counterLabel.setText(Integer.toString(messages.get()));

        HorizontalLayout layout = new HorizontalLayout(
            new Button("Start background thread", this::startBackgroundThread),
            new Button("Start background thread (worker)", this::startBackgroundWithWorker),
            counterLabel
        );
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(layout);


        messagesLayout = new VerticalLayout();
        messagesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        Div panel = new Div(messagesLayout);
        panel.setSizeFull();
        add(panel);

        setFlexGrow(1.0, panel);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        Vertx vertx = ((VertxVaadinService) attachEvent.getSession().getService()).getVertx();
        long timerId = vertx.setPeriodic(1000, tid -> ui.access(this::updateTasks));
        taskTimer = () -> {
            vertx.cancelTimer(timerId);
            bars.clear();
        };
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        taskTimer.remove();
    }

    private void updateTasks() {
        List<ProgressBar> copy = new ArrayList<>(bars);
        for (ProgressBar bar : copy) {
            double nextValue = bar.getValue() + 1;
            if (nextValue < bar.getMax()) {
                bar.setValue(nextValue);
            } else {
                bar.setValue(bar.getMax());
                bars.remove(bar);
            }
        }
    }

    private void startBackgroundWithWorker(ClickEvent<Button> event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);
        getUI().ifPresent(ui -> new UIProxy(ui).runLater(() -> {
            int current = counter.incrementAndGet();
            Runnable stopTask = addTask(current, sleep, ui);
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
            }
            stopTask.run();
        }));
    }

    private void startBackgroundThread(ClickEvent<Button> event) {
        int sleep = ThreadLocalRandom.current().nextInt(5, 20);

        Vertx vertx = ((VertxVaadinService) VaadinService.getCurrent()).getVertx();
        getUI().ifPresent(ui -> vertx.setTimer(1, i -> {
            int current = counter.incrementAndGet();
            Runnable stopTask = addTask(current, sleep, ui);
            vertx.setTimer(sleep * 1000, x -> stopTask.run());
        }));
    }

    private Runnable addTask(int taskId, int taskTimeout, UI ui) {
        ProgressBar progressBar = new ProgressBar(0, taskTimeout, 0);
        ui.access(() -> {
            updateCounter();
            HorizontalLayout l = new HorizontalLayout();
            Span label = new Span(String.format("Task %d (completion time %d s)", taskId, taskTimeout));
            label.getElement().getStyle().set("white-space", "nowrap");
            l.add(label);
            l.addAndExpand(progressBar);
            messagesLayout.addComponentAtIndex(0, l);
            bars.add(progressBar);

        });
        return () -> {
            messages.incrementAndGet();
            ui.access(() -> {
                progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
                progressBar.setValue(taskTimeout);
                bars.remove(progressBar);
                updateCounter();
            });
        };
    }


    private void updateCounter() {
        counterLabel.setText(String.format("%d of %d",
            messages.get(), counter.get()
        ));
    }
}
