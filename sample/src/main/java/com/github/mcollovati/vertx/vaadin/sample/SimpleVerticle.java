package com.github.mcollovati.vertx.vaadin.sample;

import com.github.mcollovati.vertx.vaadin.VaadinVerticle;
import com.github.mcollovati.vertx.vaadin.VaadinVerticleConfiguration;
import com.github.mcollovati.vertx.vaadin.VertxVaadin;
import com.vaadin.annotations.VaadinServletConfiguration;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.sstore.SessionStore;
import sun.security.krb5.internal.crypto.crc32;

/**
 * Created by marco on 22/07/16.
 */
@VaadinVerticleConfiguration(mountPoint = "/simple", serviceName = "SimpleVerticleVaadinService")
@VaadinServletConfiguration(ui = SimpleUI.class, productionMode = false)
public class SimpleVerticle extends VaadinVerticle {

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        context.config().put("httpPort", 9090);
    }

}
