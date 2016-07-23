package com.vaadin.demo.dashboard;

import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.vaadin.annotations.VaadinServletConfiguration;

/**
 * Created by marco on 23/07/16.
 */
@VaadinServletConfiguration(productionMode = false, ui = DashboardUI.class)
public class DashboardDemoVerticle extends VaadinVerticle {

    @Override
    protected void verticleInitialized() {
        super.verticleInitialized();
        getService().addSessionInitListener(new DashboardSessionInitListener());
    }
}
