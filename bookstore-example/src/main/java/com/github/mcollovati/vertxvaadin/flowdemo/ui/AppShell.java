package com.github.mcollovati.vertxvaadin.flowdemo.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
@Theme(themeClass = Lumo.class)
@PWA(name = "Bookstore", shortName = "Bookstore")
public class AppShell implements AppShellConfigurator {
}
