package com.github.mcollovati.vaadin.exampleapp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.mcollovati.vaadin.exampleapp.samples.MainScreen;
import com.github.mcollovati.vaadin.exampleapp.samples.authentication.AccessControl;
import com.github.mcollovati.vaadin.exampleapp.samples.authentication.BasicAccessControl;
import com.github.mcollovati.vaadin.exampleapp.samples.authentication.LoginScreen;
import com.github.mcollovati.vaadin.exampleapp.samples.authentication.LoginScreen.LoginListener;
import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.communication.SockJSPushConnection;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Main UI class of the application that shows either the login screen or the
 * main view of the application depending on whether a user is signed in.
 *
 * The @Viewport annotation configures the viewport meta tags appropriately on
 * mobile devices. Instead of device based scaling (default), using responsive
 * layouts.
 */
@Viewport("user-scalable=no,initial-scale=1.0")
@Theme("mytheme")
@Widgetset("com.github.mcollovati.vaadin.exampleapp.MyAppWidgetset")
@Push
public class MyUI extends UI {

    private AccessControl accessControl = new BasicAccessControl();
    private transient ScheduledExecutorService scheduledExecutorService;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        setPushConnection(new SockJSPushConnection(this));
        Responsive.makeResponsive(this);
        setLocale(vaadinRequest.getLocale());
        getPage().setTitle("My");
        if (!accessControl.isUserSignedIn()) {
            setContent(new LoginScreen(accessControl, new LoginListener() {
                @Override
                public void loginSuccessful() {
                    showMainView();
                }
            }));
        } else {
            showMainView();
        }
    }

    @Override
    public void attach() {
        super.attach();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        scheduledExecutorService.schedule(
            () -> {
                System.out.println("========== Schedule " + getUI());
                getUI().access(() -> {
                    System.out.println("================= Access " + getUI());
                    getUI().getPage().setTitle("Changed");
                });
            }
            , 15, TimeUnit.SECONDS);
    }

    @Override
    public void detach() {
        super.detach();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    protected void showMainView() {
        addStyleName(ValoTheme.UI_WITH_MENU);
        setContent(new MainScreen(MyUI.this));
        getNavigator().navigateTo(getNavigator().getState());
    }

    public AccessControl getAccessControl() {
        return accessControl;
    }

    public static MyUI get() {
        return (MyUI) UI.getCurrent();
    }

    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class ExampleUIVerticle extends VaadinVerticle {
    }
}
