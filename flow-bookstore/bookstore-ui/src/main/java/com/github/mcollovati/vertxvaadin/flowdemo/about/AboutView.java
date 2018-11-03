package com.github.mcollovati.vertxvaadin.flowdemo.about;

import com.github.mcollovati.vertx.vaadin.VertxVaadin;
import com.github.mcollovati.vertxvaadin.flowdemo.MainLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Version;
import io.vertx.core.impl.launcher.commands.VersionCommand;

@Route(value = "About", layout = MainLayout.class)
@PageTitle("About")
public class AboutView extends VerticalLayout {

    public static final String VIEW_NAME = "About";

    public AboutView() {
        add(new HorizontalLayout(
            VaadinIcon.INFO_CIRCLE.create(),
            new Span(" This application is using Vaadin Flow " + Version.getFullVersion())
        ));
        add(new Span("running on top of Vert.x " + VersionCommand.getVersion()));
        add(new Span("using Vertx-Vaadin-Flow " + VertxVaadin.getVersion()));

        add(buildLink("http://vaadin.com/", "Vaadin web page"));
        add(buildLink("http://vertx.io/", "Vert.x web page"));
        add(buildLink("https://github.com/mcollovati/vertx-vaadin", "Vertx-Vaadin web page"));


        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
    }

    private Anchor buildLink(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.setTarget("_blank");
        return anchor;
    }

}
