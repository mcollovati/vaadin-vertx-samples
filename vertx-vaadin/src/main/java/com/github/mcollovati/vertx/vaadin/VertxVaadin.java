package com.github.mcollovati.vertx.vaadin;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import com.github.mcollovati.vertx.vaadin.communication.SockJSPushHandler;
import com.github.mcollovati.vertx.web.sstore.SessionStoreAdapter;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.ServiceException;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_SESSION_TIMEOUT;

public class VertxVaadin {

    private final VertxVaadinService service;
    private final JsonObject config;
    private final Vertx vertx;
    private final Router router;
    //private final Handler<ServerWebSocket> webSocketHandler;


    private VertxVaadin(Vertx vertx, Optional<SessionStore> sessionStore, JsonObject config) {
        this.vertx = Objects.requireNonNull(vertx);
        this.config = Objects.requireNonNull(config);
        this.service = createVaadinService();
        try {
            service.init();
        } catch (Exception ex) {
            throw new VertxException("Cannot initialize Vaadin service", ex);
        }
        SessionStore adaptedSessionStore = SessionStoreAdapter.adapt(service, sessionStore.orElseGet(this::createSessionStore));
        this.router = initRouter(adaptedSessionStore);
        //this.webSocketHandler = initWebSocketHandler(this.router, adaptedSessionStore);
    }

    protected VertxVaadin(Vertx vertx, SessionStore sessionStore, JsonObject config) {
        this(vertx, Optional.of(sessionStore), config);
    }

    protected VertxVaadin(Vertx vertx, JsonObject config) {
        this(vertx, Optional.empty(), config);
    }

    public Router router() {
        return router;
    }

    /*
    public Handler<ServerWebSocket> webSocketHandler() {
        return webSocketHandler;
    }
    */

    public final Vertx vertx() {
        return vertx;
    }

    public final VertxVaadinService vaadinService() {
        return service;
    }

    // TODO:
    public String serviceName() {
        return config.getString("serviceName", getClass().getName());
    }

    protected final JsonObject config() {
        return config;
    }

    protected void serviceInitialized(Router router) {
    }

    protected VertxVaadinService createVaadinService() {
        return new VertxVaadinService(this, createDeploymentConfiguration());
    }

    protected SessionStore createSessionStore() {
        if (vertx.isClustered()) {
            return ClusteredSessionStore.create(vertx);
        }
        return LocalSessionStore.create(vertx);
    }

    private Router initRouter(SessionStore sessionStore) {

        String sessionCookieName = sessionCookieName();
        ////SessionStore sessionStoreAdapter = SessionStoreAdapter.adapt(vertx, sessionStore);
        SessionHandler sessionHandler = SessionHandler.create(sessionStore)
            .setSessionTimeout(config().getLong("sessionTimeout", DEFAULT_SESSION_TIMEOUT))
            .setSessionCookieName(sessionCookieName)
            .setCookieHttpOnlyFlag(true);

        Router vaadinRouter = Router.router(vertx);
        vaadinRouter.route().handler(CookieHandler.create());
        vaadinRouter.route("/VAADIN/*").handler(StaticHandler.create("VAADIN", getClass().getClassLoader()));
        vaadinRouter.route().handler(BodyHandler.create());
        vaadinRouter.route().handler(sessionHandler);


        initSockJS(vaadinRouter);

        vaadinRouter.route("/*").handler(routingContext -> {
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



        serviceInitialized(vaadinRouter);
        return vaadinRouter;
    }

    private void initSockJS(Router vaadinRouter) {
        SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);
        /*
        sockJSHandler.socketHandler(sockJSSocket -> {

            System.out.printf("========================= CONNECTING");
            sockJSSocket.handler(event -> System.out.println("GOT REQ "
                + event.toString(Charset.forName("UTF-8")))
            );
        });
        */

        SockJSPushHandler pushHandler = new SockJSPushHandler(service, sockJSHandler);
        vaadinRouter.route("/PUSH/*").handler(pushHandler);
        //vaadinRouter.route("/PUSH/*").handler(sockJSHandler);
    }



    private String sessionCookieName() {
        return config().getString("sessionCookieName", "vertx-web.session");
    }

    /*
    private Handler<ServerWebSocket> initWebSocketHandler(Router vaadinRouter, SessionStore sessionStore) {

        String mountPoint = config().getString("mountPoint", "/");
        String sessionCookieName = sessionCookieName();
        WebsocketSessionHandler.WebsocketSessionHandlerBuilder websocketSessionHandlerBuilder =
            WebsocketSessionHandler.builder().mountPoint(mountPoint)
                .cookieName(sessionCookieName).sessionStore(sessionStore);

        AtmosphereCoordinator atmosphereCoordinator = initAtmosphere(vaadinRouter, service);

        router.get("/PUSH").handler(atmosphereCoordinator::route);
        router.post("/PUSH").handler(atmosphereCoordinator::route);
        return websocketSessionHandlerBuilder.next(atmosphereCoordinator::route).build();
    }


    private AtmosphereCoordinator initAtmosphere(Router router, VertxVaadinService service) {
        final String bufferSize = String.valueOf(PushConstants.WEBSOCKET_BUFFER_SIZE);

        AtmosphereInterceptor trackMessageSize = new TrackMessageSizeInterceptor();

        VertxAtmosphere.Builder pushBuilder = new VertxAtmosphere.Builder()
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

        AtmosphereCoordinator atmosphereCoordinator = ExposeAtmosphere.newCoordinator(pushBuilder);
        AtmosphereFramework framework = atmosphereCoordinator.framework();
        trackMessageSize.configure(framework.getAtmosphereConfig());

        VertxPushHandler vertxPushHandler = new VertxPushHandler(service);
        vertxPushHandler.setLongPollingSuspendTimeout(framework.getAtmosphereConfig()
            .getInitParameter(com.vaadin.server.Constants.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING, -1));

        PushAtmosphereHandler pushAtmosphereHandler = new PushAtmosphereHandler();
        pushAtmosphereHandler.setPushHandler(vertxPushHandler);
        framework.addAtmosphereHandler("/*", pushAtmosphereHandler);

        atmosphereCoordinator.ready();

        service.addServiceDestroyListener(event -> atmosphereCoordinator.shutdown());

        return atmosphereCoordinator;
    }

    */
    private DefaultDeploymentConfiguration createDeploymentConfiguration() {
        return new DefaultDeploymentConfiguration(getClass(), initProperties());
    }

    private Properties initProperties() {
        Properties initParameters = new Properties();
        //readUiFromEnclosingClass(initParameters);
        //readConfigurationAnnotation(initParameters);
        initParameters.putAll(config().getMap());
        return initParameters;
    }

    // TODO: change JsonObject to VaadinOptions interface
    public static VertxVaadin create(Vertx vertx, SessionStore sessionStore, JsonObject config) {
        return new VertxVaadin(vertx, sessionStore, config);
    }

    public static VertxVaadin create(Vertx vertx, JsonObject config) {
        return new VertxVaadin(vertx, config);
    }

    /*
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
    */
}
