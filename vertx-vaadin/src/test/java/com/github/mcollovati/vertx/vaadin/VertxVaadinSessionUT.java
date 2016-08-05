package com.github.mcollovati.vertx.vaadin;

import com.vaadin.server.DefaultDeploymentConfiguration;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

/**
 * Created by marco on 27/07/16.
 */
@RunWith(VertxUnitRunner.class)
@Ignore
public class VertxVaadinSessionUT {

    Vertx vertx;
    VertxVaadinService vaadinService;

    @Before
    public void setUp(TestContext context) {
        vertx = vertx = Vertx.vertx();
        VaadinVerticle verticle = new VaadinVerticle();
        verticle.init(vertx, vertx.getOrCreateContext());
        vaadinService = new VertxVaadinService(verticle, mock(DefaultDeploymentConfiguration.class));
    }

    public void vertxVaadinSessionShouldReceiveSessionExpiredEvents() {


    }

}
