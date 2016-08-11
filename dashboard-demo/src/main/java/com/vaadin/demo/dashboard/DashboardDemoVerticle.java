package com.vaadin.demo.dashboard;

import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.vaadin.annotations.VaadinServletConfiguration;
import io.vertx.ext.web.Router;

/**
 * Created by marco on 23/07/16.
 */
@VaadinServletConfiguration(productionMode = false, ui = DashboardUI.class)
public class DashboardDemoVerticle extends VaadinVerticle {

    @Override
    protected void serviceInitialized(VertxVaadinService service, Router router) {
        service.addSessionInitListener(new DashboardSessionInitListener());
    }
}
