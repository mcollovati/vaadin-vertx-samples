package com.github.mcollovati.vertxvaadin.flowdemo;

import com.github.mcollovati.vertxvaadin.flowdemo.about.AboutView;
import com.github.mcollovati.vertxvaadin.flowdemo.crud.SampleCrudView;
import com.github.mcollovati.vertxvaadin.flowdemo.push.PushView;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.dependency.CssImport;

//import com.vaadin.flow.server.PWA;

/**
 * The layout of the pages e.g. About and Inventory.
 */
@CssImport("./styles/shared-styles.css")
//@JsModule("./styles/shared-styles.js")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@PWA(name = "Book store sample", shortName = "BookStore")
@Push
public class MainLayout extends FlexLayout implements RouterLayout {
    private Menu menu;

    public MainLayout() {
        setSizeFull();
        setClassName("main-layout");

        menu = new Menu();
        menu.addView(SampleCrudView.class, SampleCrudView.VIEW_NAME,
                VaadinIcon.EDIT.create());
        menu.addView(AboutView.class, AboutView.VIEW_NAME,
                VaadinIcon.INFO_CIRCLE.create());
        menu.addView(PushView.class, PushView.VIEW_NAME,
            VaadinIcon.REFRESH.create());

        add(menu);
    }
}
