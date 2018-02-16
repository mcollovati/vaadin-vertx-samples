package com.github.mcollovati.vertx.vaadin.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.mcollovati.vertx.vaadin.VertxVaadinRequest;
import com.github.mcollovati.vertx.vaadin.VertxVaadinService;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.LegacyCommunicationManager;
import com.vaadin.server.ServiceException;
import com.vaadin.server.ServletPortletHelper;
import com.vaadin.server.SessionExpiredException;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.ExposeVaadinCommunicationPkg;
import com.vaadin.server.communication.PushConnection;
import com.vaadin.server.communication.ServerRpcHandler;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;
import elemental.json.JsonException;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

/**
 * Source code adapted from Vaadin PushHandler
 */
public class SockJSPushHandler implements Handler<RoutingContext> {

    private final VertxVaadinService service;
    private final SockJSHandler sockJSHandler;
    /**
     * Callback used when we receive a request to establish a push channel for a
     * UI. Associate the AtmosphereResource with the UI and leave the connection
     * open by calling resource.suspend(). If there is a pending push, send it
     * now.
     */
    private final PushEventCallback establishCallback = (SockJSSocket socket, UI ui, Buffer data) -> {
        getLogger().log(Level.FINER,
            "New push connection for resource {0} with transport {1}",
            new Object[]{"socket.uuid()", "resource.transport()"});


        // TODO: verify
        //resource.getResponse().setContentType("text/plain; charset=UTF-8");

        VaadinSession session = ui.getSession();


        /* TODO: verify
        String requestToken = resource.getRequest()
            .getParameter(ApplicationConstants.PUSH_ID_PARAMETER);
        if (!isPushIdValid(session, requestToken)) {
            getLogger().log(Level.WARNING,
                "Invalid identifier in new connection received from {0}",
                resource.getRequest().getRemoteHost());
            // Refresh on client side, create connection just for
            // sending a message
            sendRefreshAndDisconnect(resource);
            return;
        }
        */

        // TODO: verify
        //suspend(resource);

        SockJSPushConnection connection = getConnectionForUI(ui);
        assert (connection != null);
        connection.connect(socket);
    };
    /**
     * Callback used when we receive a UIDL request through Atmosphere. If the
     * push channel is bidirectional (websockets), the request was sent via the
     * same channel. Otherwise, the client used a separate AJAX request. Handle
     * the request and send changed UI state via the push channel (we do not
     * respond to the request directly.)
     */
    private final PushEventCallback receiveCallback = (
        SockJSSocket socket, UI ui, Buffer message) -> {

        //getLogger().log(Level.FINER, "Received message from resource {0}",
        //    resource.uuid());

        SockJSPushConnection connection = getConnectionForUI(ui);

        assert connection != null : "Got push from the client "
            + "even though the connection does not seem to be "
            + "valid. This might happen if a HttpSession is "
            + "serialized and deserialized while the push "
            + "connection is kept open or if the UI has a "
            + "connection of unexpected type.";


        /* Verify
        Reader reader = connection.receiveMessage(req.getReader());
        if (reader == null) {
            // The whole message was not yet received
            return;
        }
        */
        Reader reader = new StringReader(message.toString());

        // Should be set up by caller
        VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
        assert vaadinRequest != null;

        try {
            new ServerRpcHandler().handleRpc(ui, reader, vaadinRequest);
            connection.push(false);
        } catch (JsonException e) {
            getLogger().log(Level.SEVERE, "Error writing JSON to response", e);
            // Refresh on client side
            sendRefreshAndDisconnect(socket);
        } catch (LegacyCommunicationManager.InvalidUIDLSecurityKeyException e) {
            getLogger().log(Level.WARNING,
                "Invalid security key received from {0}",
                socket.remoteAddress());
            // Refresh on client side
            sendRefreshAndDisconnect(socket);
        }
    };

    public SockJSPushHandler(VertxVaadinService service, SockJSHandler sockJSHandler) {
        this.service = service;
        this.sockJSHandler = sockJSHandler;
        this.sockJSHandler.socketHandler(this::onConnect);
    }

    private void onConnect(SockJSSocket socket) {
        RoutingContext routingContext = CurrentInstance.get(RoutingContext.class);
        callWithUi(socket, routingContext, establishCallback, null);

        socket.handler(data -> {

            onMessage(socket, routingContext, data);
        });
    }

    private void onMessage(SockJSSocket socket, RoutingContext routingContext, Buffer data) {
        System.out.println("ON data current is " + routingContext);
        callWithUi(socket, routingContext, receiveCallback, data);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        CurrentInstance.set(RoutingContext.class, routingContext);
        VertxVaadinRequest vaadinRequest = new VertxVaadinRequest(service, routingContext);
        try {
            sockJSHandler.handle(routingContext);
        } finally {
            CurrentInstance.set(RoutingContext.class, null);
        }

    }

    private void callWithUi(final SockJSSocket socket, RoutingContext routingContext,
                            final PushEventCallback callback, Buffer message) {

        VertxVaadinRequest vaadinRequest = new VertxVaadinRequest(service, routingContext);
        VaadinSession session = null;

        service.requestStart(vaadinRequest, null);
        try {
            try {
                session = service.findVaadinSession(vaadinRequest);
                assert VaadinSession.getCurrent() == session;

            } catch (ServiceException e) {
                getLogger().log(Level.SEVERE,
                    "Could not get session. This should never happen", e);
                return;
            } catch (SessionExpiredException e) {
                SystemMessages msg = service
                    .getSystemMessages(ServletPortletHelper.findLocale(null,
                        null, vaadinRequest), vaadinRequest);
                sendNotificationAndDisconnect(socket,
                    VaadinService.createCriticalNotificationJSON(
                        msg.getSessionExpiredCaption(),
                        msg.getSessionExpiredMessage(), null,
                        msg.getSessionExpiredURL()));
                return;
            }

            UI ui = null;
            session.lock();
            try {
                ui = service.findUI(vaadinRequest);
                assert UI.getCurrent() == ui;

                if (ui == null) {
                    sendNotificationAndDisconnect(
                        socket, ExposeVaadinCommunicationPkg.getUINotFoundErrorJSON(service, vaadinRequest)
                    );
                } else {
                    callback.run(socket, ui, message);
                }
            } catch (final IOException e) {
                callErrorHandler(session, e);
            } catch (final Exception e) {
                SystemMessages msg = service
                    .getSystemMessages(ServletPortletHelper.findLocale(null,
                        null, vaadinRequest), vaadinRequest);


                /* TODO: verify
                AtmosphereResource errorResource = resource;
                if (ui != null && ui.getPushConnection() != null) {
                    // We MUST use the opened push connection if there is one.
                    // Otherwise we will write the response to the wrong request
                    // when using streaming (the client -> server request
                    // instead of the opened push channel)
                    errorResource = ((AtmospherePushConnection) ui
                        .getPushConnection()).getResource();
                }
                */

                sendNotificationAndDisconnect(socket,
                    VaadinService.createCriticalNotificationJSON(
                        msg.getInternalErrorCaption(),
                        msg.getInternalErrorMessage(), null,
                        msg.getInternalErrorURL()));
                callErrorHandler(session, e);
            } finally {
                try {
                    session.unlock();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING,
                        "Error while unlocking session", e);
                    // can't call ErrorHandler, we (hopefully) don't have a lock
                }
            }
        } finally {
            try {
                service.requestEnd(vaadinRequest, null, session);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error while ending request", e);

                // can't call ErrorHandler, we don't have a lock
            }
        }
    }

    /**
     * Call the session's {@link ErrorHandler}, if it has one, with the given
     * exception wrapped in an {@link ErrorEvent}.
     */
    private void callErrorHandler(VaadinSession session, Exception e) {
        try {
            ErrorHandler errorHandler = ErrorEvent.findErrorHandler(session);
            if (errorHandler != null) {
                errorHandler.error(new ErrorEvent(e));
            }
        } catch (Exception ex) {
            // Let's not allow error handling to cause trouble; log fails
            getLogger().log(Level.WARNING, "ErrorHandler call failed", ex);
        }
    }

    /**
     * Tries to send a critical notification to the client and close the
     * connection. Does nothing if the connection is already closed.
     */
    private static void sendNotificationAndDisconnect(
        SockJSSocket socket, String notificationJson) {
        // TODO Implemented differently from sendRefreshAndDisconnect
        try {
            socket.write(Buffer.buffer(notificationJson, "UTF-8"));
            socket.resume();
        } catch (Exception e) {
            getLogger().log(Level.FINEST,
                "Failed to send critical notification to client", e);
        }
    }

    private static final Logger getLogger() {
        return Logger.getLogger(SockJSPushHandler.class.getName());
    }

    private static SockJSPushConnection getConnectionForUI(UI ui) {
        PushConnection pushConnection = ui.getPushConnection();
        if (pushConnection instanceof SockJSPushConnection) {
            return (SockJSPushConnection) pushConnection;
        } else {
            return null;
        }
    }

    private static void sendRefreshAndDisconnect(SockJSSocket socket) {
        sendNotificationAndDisconnect(socket, VaadinService
            .createCriticalNotificationJSON(null, null, null, null));
    }

    private interface PushEventCallback {
        void run(SockJSSocket socket, UI ui, Buffer message) throws IOException;
    }


}
