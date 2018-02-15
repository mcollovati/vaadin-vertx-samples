package com.github.mcollovati.vertx.client;

import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ResourceLoader;
import com.vaadin.client.ValueMap;
import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.PushConnection;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.Version;
import com.vaadin.shared.ui.ui.UIConstants;
import com.vaadin.shared.ui.ui.UIState;
import com.vaadin.shared.util.SharedUtil;
import elemental.json.JsonObject;

public class SockJSPushConnection implements PushConnection {

    private ApplicationConnection connection;
    private SockJS socket;
    private SockJSConfiguration config;
    private State state = State.CONNECTING;
    private String uri;
    private String transport;

    /**
     * Keeps track of the disconnect confirmation command for cases where
     * pending messages should be pushed before actually disconnecting.
     */
    private Command pendingDisconnectCommand;

    /**
     * The url to use for push requests
     */
    private String url;

    @Override
    public void init(ApplicationConnection connection, UIState.PushConfigurationState pushConfiguration) {
        this.connection = connection;

        connection.addHandler(ApplicationConnection.ApplicationStoppedEvent.TYPE,
            event -> {
                if (state == State.CLOSING
                    || state == State.CLOSED) {
                    return;
                }

                disconnect(() -> {
                });
            });

        config = createConfig();
        /*
        String debugParameter = Window.Location.getParameter("debug");
        if ("push".equals(debugParameter)) {
            config.setStringValue("logLevel", "debug");
        }
        for (String param : pushConfiguration.parameters.keySet()) {
            String value = pushConfiguration.parameters.get(param);
            if (value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("false")) {
                config.setBooleanValue(param, value.equalsIgnoreCase("true"));
            } else {
                config.setStringValue(param, value);
            }
        }
        */
        if (pushConfiguration.pushUrl != null) {
            url = pushConfiguration.pushUrl;
        } else {
            url = ApplicationConstants.APP_PROTOCOL_PREFIX
                + ApplicationConstants.PUSH_PATH;
        }
        runWhenSockJSLoaded(
            () -> Scheduler.get().scheduleDeferred(this::connect));

    }


    @Override
    public void push(JsonObject message) {
        if (!isBidirectional()) {
            throw new IllegalStateException(
                "This server to client push connection should not be used to send client to server messages");
        }
        if (state == State.OPEN) {
            getLogger().info("Sending push (" + transport
                + ") message to server: " + message.toJson());

            doPush(socket, message.toJson());
            return;
        }

        if (state == State.CONNECTING) {
            getConnectionStateHandler().pushNotConnected(message);
            return;
        }

        throw new IllegalStateException("Can not push after disconnecting");

    }

    private native void doPush(SockJS socket, String message)
    /*-{
       socket.send(message);
    }-*/;

    @Override
    public boolean isActive() {
        switch (state) {
            case CONNECTING:
            case OPEN:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void disconnect(Command command) {
        assert command != null;

        switch (state) {
            case CONNECTING:
                // Make the connection callback initiate the disconnection again
                state = State.CLOSING;
                pendingDisconnectCommand = command;
                break;
            case OPEN:
                // Normal disconnect
                getLogger().info("Closing push connection");
                doDisconnect(socket);
                state = State.CLOSED;
                command.execute();
                break;
            case CLOSING:
            case CLOSED:
                throw new IllegalStateException(
                    "Can not disconnect more than once");
        }

    }

    @Override
    public String getTransportType() {
        return socket.getTransport();
    }

    @Override
    public boolean isBidirectional() {
        if (transport == null) {
            return false;
        }

        if (!transport.equals("websocket")) {
            // If we are not using websockets, we want to send XHRs
            return false;
        }
        if (getPushConfigurationState().alwaysUseXhrForServerRequests) {
            // If user has forced us to use XHR, let's abide
            return false;
        }
        if (state == State.CONNECTING) {
            // Not sure yet, let's go for using websockets still as still will
            // delay the message until a connection is established. When the
            // connection is established, bi-directionality will be checked
            // again to be sure
        }
        return true;

    }

    protected SockJSConfiguration getConfig() {
        return config;
    }

    private void connect() {
        String baseUrl = connection.translateVaadinUri(url);
        String extraParams = UIConstants.UI_ID_PARAMETER + "="
            + connection.getConfiguration().getUIId();

        String pushId = connection.getMessageHandler().getPushId();
        if (pushId != null) {
            extraParams += "&" + ApplicationConstants.PUSH_ID_PARAMETER + "="
                + pushId;
        }

        // uri is needed to identify the right connection when closing
        uri = SharedUtil.addGetParameters(baseUrl, extraParams);

        getLogger().info("Establishing push connection");
        socket = doConnect(uri, getConfig());
    }


    protected native SockJSConfiguration createConfig()
    /*-{
        return {
            transports: ['websocket', ]
        };
    }-*/;


    protected void onOpen(JavaScriptObject event) {
        getLogger().info(
            "Push connection established using " + socket.getTransport());
        onConnect(socket);
    }

    protected void onError(JavaScriptObject response) {
        state = State.CLOSED;
        getConnectionStateHandler().pushError(this, response);
        // TODO - reconnect
    }

    protected void onClose(JavaScriptObject response) {
        state = State.CONNECTING;
        getConnectionStateHandler().pushClosed(this, response);
    }

    protected void onMessage(TransportMessageEvent response) {
        String message = response.getResponseBody();
        ValueMap json = MessageHandler.parseWrappedJson(message);
        if (json == null) {
            // Invalid string (not wrapped as expected)
            getConnectionStateHandler().pushInvalidContent(this, message);
            return;
        } else {
            getLogger().info("Received push (" + getTransportType()
                + ") message: " + message);
            connection.getMessageHandler().handleMessage(json);
        }
    }


    protected void onConnect(SockJS socket) {
        transport = socket.getTransport();
        switch (state) {
            case CONNECTING:
                state = State.OPEN;
                getConnectionStateHandler().pushOk(this);
                break;
            case CLOSING:
                // Set state to connected to make disconnect close the connection
                state = State.OPEN;
                assert pendingDisconnectCommand != null;
                disconnect(pendingDisconnectCommand);
                break;
            case OPEN:
                // IE likes to open the same connection multiple times, just ignore
                break;
            default:
                throw new IllegalStateException(
                    "Got onOpen event when conncetion state is " + state
                        + ". This should never happen.");
        }
    }

    private native SockJS doConnect(String uri,
                                    JavaScriptObject config)
    /*-{
        var self = this;


        config.url = uri;
        config.onOpen = $entry(function(response) {
            self.@com.github.mcollovati.vaadin.sockjs.client.SockJSPushConnection::onOpen(*)(response);
        });
        config.onMessage = $entry(function(response) {
            self.@com.github.mcollovati.vaadin.sockjs.client.SockJSPushConnection::onMessage(*)(response);
        });
        config.onError = $entry(function(response) {
            self.@com.github.mcollovati.vaadin.sockjs.client.SockJSPushConnection::onError(*)(response);
        });
        config.onClose = $entry(function(response) {
            self.@com.github.mcollovati.vaadin.sockjs.client.SockJSPushConnection::onClose(*)(response);
        });


        return $wnd.vaadinPush.SockJS.connect(config);
    }-*/;

    private void runWhenSockJSLoaded(final Command command) {
        if (isSockJSLoaded()) {
            command.execute();
        } else {
            final String pushJs = getVersionedPushJs();

            getLogger().info("Loading " + pushJs);
            ResourceLoader.get().loadScript(
                connection.getConfiguration().getVaadinDirUrl() + pushJs,
                new ResourceLoader.ResourceLoadListener() {
                    @Override
                    public void onLoad(ResourceLoader.ResourceLoadEvent event) {
                        if (isSockJSLoaded()) {
                            getLogger().info(pushJs + " loaded");
                            command.execute();
                        } else {
                            // If bootstrap tried to load vaadinPush.js,
                            // ResourceLoader assumes it succeeded even if
                            // it failed (#11673)
                            onError(event);
                        }
                    }

                    @Override
                    public void onError(ResourceLoader.ResourceLoadEvent event) {
                        getConnectionStateHandler().pushScriptLoadError(
                            event.getResourceUrl());
                    }
                });
        }
    }

    private UIState.PushConfigurationState getPushConfigurationState() {
        return connection.getUIConnector().getState().pushConfiguration;
    }

    private ConnectionStateHandler getConnectionStateHandler() {
        return connection.getConnectionStateHandler();
    }

    private String getVersionedPushJs() {
        String pushJs;
        if (ApplicationConfiguration.isProductionMode()) {
            pushJs = "vaadinPushSockJS.js";
        } else {
            pushJs = "vaadinPushSockJS.js";
        }
        // Parameter appended to bypass caches after version upgrade.
        pushJs += "?v=" + Version.getFullVersion();
        return pushJs;
    }

    private static native boolean isSockJSLoaded()
    /*-{
        return $wnd.vaadinPush && $wnd.vaadinPush.SockJS;
    }-*/;

    private static native void doDisconnect(SockJS sock)
    /*-{
       sock.close(s);
    }-*/;


    private static Logger getLogger() {
        return Logger.getLogger(SockJSPushConnection.class.getName());
    }

    private enum State {
        CONNECTING, OPEN, CLOSING, CLOSED
    }

    public static class SockJSConfiguration extends JavaScriptObject {
        protected SockJSConfiguration() {}
    }

    public static class TransportMessageEvent extends JavaScriptObject {

        protected TransportMessageEvent() {}
        
        protected final native String getResponseBody()
        /*-{
           return this.transport;
         }-*/;

    }

    public static class SockJS extends JavaScriptObject {

        protected SockJS() {}
        
        protected final native String getTransport()
        /*-{
           return this.transport;
         }-*/;

        protected final native int readyState()
         /*-{
            return this.readyState;
          }-*/;

    }


}
