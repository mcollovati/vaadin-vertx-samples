package com.github.mcollovati.vertxvaadin.fusion;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the * and some desktop browsers.
 */
@PWA(name = "fusion-example", shortName = "fusion-example", offlineResources = {"images/logo.png"})
public class Application implements AppShellConfigurator {

}
