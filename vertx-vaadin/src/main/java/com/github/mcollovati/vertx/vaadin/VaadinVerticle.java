/*
 * The MIT License
 * Copyright © 2016 Marco Collovati (mcollovati@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mcollovati.vertx.vaadin;

import com.github.mcollovati.vertx.web.sstore.SessionStoreAdapter;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.PushAtmosphereHandler;
import com.vaadin.server.communication.VertxPushHandler;
import com.vaadin.shared.communication.PushConstants;
import com.vaadin.ui.UI;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.atmosphere.util.VoidAnnotationProcessor;
import org.atmosphere.vertx.AtmosphereCoordinator;
import org.atmosphere.vertx.ExposeAtmosphere;
import org.atmosphere.vertx.VertxAtmosphere;
import org.atmosphere.vertx.WebsocketSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Properties;

import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_SESSION_TIMEOUT;

/**
 * Created by marco on 16/07/16.
 */
public class VaadinVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(VaadinVerticle.class);

    private HttpServer httpServer;
    private VertxVaadinService service;


    @Override
    public void start(Future<Void> startFuture) throws Exception {

        log.info("Starting vaadin verticle " + getClass().getName());

        VaadinVerticleConfiguration vaadinVerticleConfiguration = getClass().getAnnotation(VaadinVerticleConfiguration.class);
        String mountPoint = Optional.ofNullable(vaadinVerticleConfiguration)
            .map(VaadinVerticleConfiguration::mountPoint)
            .orElse(config().getString("mountPoint", "/"));

        service = createVaadinService();
        service.init();

        HttpServerOptions serverOptions = new HttpServerOptions()
            .setCompressionSupported(true);

        httpServer = vertx.createHttpServer(serverOptions);


        String sessionCookieName = config().getString("sessionCookieName", "vertx-web.session");
        SessionStore sessionStore = SessionStoreAdapter.adapt(vertx, createSessionStore());
        SessionHandler sessionHandler = SessionHandler.create(sessionStore)
            .setSessionTimeout(config().getLong("sessionTimeout", DEFAULT_SESSION_TIMEOUT))
            .setSessionCookieName(sessionCookieName)
            .setCookieHttpOnlyFlag(true);

        WebsocketSessionHandler.WebsocketSessionHandlerBuilder websocketSessionHandlerBuilder =
            WebsocketSessionHandler.builder().mountPoint(mountPoint)
                .cookieName(sessionCookieName).sessionStore(sessionStore);


        Router vaadinRouter = Router.router(vertx);
        vaadinRouter.route().handler(CookieHandler.create());
        vaadinRouter.route("/VAADIN/*").handler(StaticHandler.create("VAADIN", getClass().getClassLoader()));
        vaadinRouter.route().handler(BodyHandler.create());
        vaadinRouter.route().handler(sessionHandler);

        vaadinRouter.route("/*").handler(routingContext -> {
            //dump(routingContext);
            HttpServerRequest req = routingContext.request();
            VertxVaadinRequest request = new VertxVaadinRequest(service, routingContext);
            VertxVaadinResponse response = new VertxVaadinResponse(service, routingContext);

            try {
                service.handleRequest(request, response);
                response.end();
            } catch (ServiceException ex) {
                routingContext.fail(ex);
            }
        });
        initPush(httpServer, vaadinRouter, service, websocketSessionHandlerBuilder);

        Router router = Router.router(vertx);
        router.mountSubRouter(mountPoint, vaadinRouter);

        httpServer.requestHandler(router::accept).listen(config().getInteger("httpPort", 8080));
        serviceInitialized(vaadinRouter);

        log.info("Started vaadin verticle " + getClass().getName());
        startFuture.complete();
    }

    protected void serviceInitialized(Router router) {
    }

    protected VertxVaadinService createVaadinService() {
        return new VertxVaadinService(this, createDeploymentConfiguration());
    }

    protected VertxVaadinService getService() {
        return service;
    }

    protected SessionStore createSessionStore() {
        if (vertx.isClustered()) {
            return ClusteredSessionStore.create(vertx);
        }
        return LocalSessionStore.create(vertx);
    }


    private void dump(RoutingContext routingContext) {
        System.out.println("URI: " + routingContext.request().uri());
        System.out.println("PATH: " + routingContext.request().path());
        System.out.println("REMOTEADDR: " + routingContext.request().remoteAddress());
        System.out.println("LOCALADDR: " + routingContext.request().localAddress());
        System.out.println("HOST: " + routingContext.request().host());
        System.out.println("CTXPATH: " + routingContext.mountPoint());
        System.out.println("NORMPATH: " + routingContext.normalisedPath());
        System.out.println("ContenType: " + routingContext.getAcceptableContentType());

    }

    private void initPush(HttpServer httpServer, Router router, VertxVaadinService service,
                          WebsocketSessionHandler.WebsocketSessionHandlerBuilder websocketSessionHandlerBuilder) {
        final String bufferSize = String.valueOf(PushConstants.WEBSOCKET_BUFFER_SIZE);

        AtmosphereInterceptor trackMessageSize = new TrackMessageSizeInterceptor();


        VertxAtmosphere.Builder pushBuilder = new VertxAtmosphere.Builder()
            .httpServer(httpServer)
            .url("/PUSH")
            .initParam(ApplicationConfig.BROADCASTER_CACHE, UUIDBroadcasterCache.class.getName())
            .initParam(ApplicationConfig.ANNOTATION_PROCESSOR, VoidAnnotationProcessor.class.getName())
            .initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true")
            .initParam(ApplicationConfig.MESSAGE_DELIMITER, String.valueOf(PushConstants.MESSAGE_DELIMITER))
            .initParam(ApplicationConfig.DROP_ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "false")
            // Disable heartbeat (it does not emit correct events client side)
            // https://github.com/Atmosphere/atmosphere-javascript/issues/141
            .initParam(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTORS, HeartbeatInterceptor.class.getName())
            .initParam(ApplicationConfig.WEBSOCKET_BUFFER_SIZE, bufferSize)
            .initParam(ApplicationConfig.WEBSOCKET_MAXTEXTSIZE, bufferSize)
            .initParam(ApplicationConfig.WEBSOCKET_MAXBINARYSIZE, bufferSize)
            .initParam(ApplicationConfig.PROPERTY_ALLOW_SESSION_TIMEOUT_REMOVAL, "false")
            // Disable Atmosphere's message about commercial support
            .initParam("org.atmosphere.cpr.showSupportMessage", "false")
            .interceptor(trackMessageSize);


        //VertxAtmosphere atmosphere = pushBuilder.build();
        AtmosphereCoordinator atmosphereCoordinator = ExposeAtmosphere.newCoordinator(pushBuilder);
        AtmosphereFramework framework = atmosphereCoordinator.framework();
        trackMessageSize.configure(framework.getAtmosphereConfig());

        VertxPushHandler vertxPushHandler = new VertxPushHandler(service);
        vertxPushHandler.setLongPollingSuspendTimeout(framework.getAtmosphereConfig()
            .getInitParameter(com.vaadin.server.Constants.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING, -1));


        PushAtmosphereHandler pushAtmosphereHandler = new PushAtmosphereHandler();
        pushAtmosphereHandler.setPushHandler(vertxPushHandler);
        framework.addAtmosphereHandler("/*", pushAtmosphereHandler);


        router.get("/PUSH").handler(atmosphereCoordinator::route);
        router.post("/PUSH").handler(atmosphereCoordinator::route);
        httpServer.websocketHandler(websocketSessionHandlerBuilder.next(atmosphereCoordinator::route).build());

        atmosphereCoordinator.ready();

        service.addServiceDestroyListener(event -> atmosphereCoordinator.shutdown());

    }


    @Override
    public void stop() throws Exception {
        log.info("Stopping vaadin verticle " + getClass().getName());
        service.destroy();
        httpServer.close();
        log.info("Stopped vaadin verticle " + getClass().getName());
    }

    private DefaultDeploymentConfiguration createDeploymentConfiguration() {
        return new DefaultDeploymentConfiguration(getClass(), initProperties());
    }

    private Properties initProperties() {
        Properties initParameters = new Properties();
        JsonObject vaadinInitParams = config().getJsonObject("vaadin", new JsonObject());
        readUiFromEnclosingClass(initParameters);
        readConfigurationAnnotation(initParameters);
        initParameters.putAll(vaadinInitParams.getMap());
        return initParameters;
    }

    private void readUiFromEnclosingClass(Properties initParameters) {
        Class<?> enclosingClass = getClass().getEnclosingClass();

        if (enclosingClass != null && UI.class.isAssignableFrom(enclosingClass)) {
            initParameters.put(VaadinSession.UI_PARAMETER,
                enclosingClass.getName());
        }
    }

    private void readConfigurationAnnotation(Properties initParameters) {

        VaadinServletConfiguration configAnnotation = getClass().getAnnotation(VaadinServletConfiguration.class);
        if (configAnnotation != null) {
            Method[] methods = VaadinServletConfiguration.class
                .getDeclaredMethods();
            for (Method method : methods) {
                VaadinServletConfiguration.InitParameterName name =
                    method.getAnnotation(VaadinServletConfiguration.InitParameterName.class);
                assert name !=
                    null : "All methods declared in VaadinServletConfiguration should have a @InitParameterName annotation";

                try {
                    Object value = method.invoke(configAnnotation);

                    String stringValue;
                    if (value instanceof Class<?>) {
                        stringValue = ((Class<?>) value).getName();
                    } else {
                        stringValue = value.toString();
                    }

                    initParameters.setProperty(name.value(), stringValue);
                } catch (Exception e) {
                    // This should never happen
                    throw new VertxException(
                        "Could not read @VaadinServletConfiguration value "
                            + method.getName(), e);
                }
            }
        }
    }

}
