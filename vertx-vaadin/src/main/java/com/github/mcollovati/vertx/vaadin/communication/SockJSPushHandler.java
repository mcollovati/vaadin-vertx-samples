package com.github.mcollovati.vertx.vaadin.communication;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
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
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;
import elemental.json.JsonException;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

/**
 * Handles incoming push connections and messages and dispatches them to the
 * correct {@link UI}/ {@link SockJSPushConnection}.
 *
 * Source code adapted from Vaadin {@link com.vaadin.server.communication.PushHandler}
 */
public class SockJSPushHandler implements Handler<RoutingContext> {

    private final VertxVaadinService service;
    private final SockJSHandler sockJSHandler;

    /**
     * Callback used when we receive a request to establish a push channel for a
     * UI. Associate the SockJS socket with the UI and leave the connection
     * open. If there is a pending push, send it now.
     */
    private final PushEventCallback establishCallback = (PushEvent event, UI ui) -> {
        getLogger().log(Level.FINER,
            "New push connection for resource {0} with transport {1}",
            new Object[]{"socket.uuid()", "resource.transport()"});


        // TODO: verify if this is needed with non websocket transports
        //event.routingContext.response().putHeader("Content-Type", "text/plain; charset=UTF-8");
        //resource.getResponse().setContentType("text/plain; charset=UTF-8");

        VaadinSession session = ui.getSession();
        SockJSSocket socket = event.socket;

        HttpServerRequest request = event.routingContext.request();

        String requestToken = request.getParam(ApplicationConstants.PUSH_ID_PARAMETER);
        if (!isPushIdValid(session, requestToken)) {
            getLogger().log(Level.WARNING,
                "Invalid identifier in new connection received from {0}",
                socket.remoteAddress());
            // Refresh on client side, create connection just for
            // sending a message
            sendRefreshAndDisconnect(socket);
            return;
        }

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
    private final PushEventCallback receiveCallback = (PushEvent event, UI ui) -> {

        //getLogger().log(Level.FINER, "Received message from resource {0}",
        //    resource.uuid());

        SockJSSocket socket = event.socket;
        SockJSPushConnection connection = getConnectionForUI(ui);

        assert connection != null : "Got push from the client "
            + "even though the connection does not seem to be "
            + "valid. This might happen if a HttpSession is "
            + "serialized and deserialized while the push "
            + "connection is kept open or if the UI has a "
            + "connection of unexpected type.";


        Reader reader = event.message().map(connection::receiveMessage).orElse(null);
        if (reader == null) {
            // The whole message was not yet received
            return;
        }


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
        callWithUi(new PushEvent(socket, routingContext, null), establishCallback);

        socket.handler(data -> onMessage(new PushEvent(socket, routingContext, data)));
        socket.exceptionHandler(t -> onError(new PushEvent(socket, routingContext, null), t));
    }


    private void onError(PushEvent ev, Throwable t) {
        callWithUi(ev, ((event, ui) -> {
            // Should be set up by caller
            VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
            assert vaadinRequest != null;
            VaadinSession vaadinSession = VaadinSession.getCurrent();

            SystemMessages msg = service
                .getSystemMessages(ServletPortletHelper.findLocale(null,
                    null, vaadinRequest), vaadinRequest);
            SockJSSocket errorSocket = getOpenedPushConnection(ev.socket(), ui);
            sendNotificationAndDisconnect(errorSocket,
                VaadinService.createCriticalNotificationJSON(
                    msg.getInternalErrorCaption(),
                    msg.getInternalErrorMessage(), null,
                    msg.getInternalErrorURL()));
            if (t instanceof Exception) {
                callErrorHandler(vaadinSession, (Exception) t);
            } else {
                getLogger().log(Level.WARNING, "ErrorHandler cannot handle this throwable", t);
            }
        }));
    }

    private void onMessage(PushEvent event) {
        callWithUi(event, receiveCallback);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        CurrentInstance.set(RoutingContext.class, routingContext);
        try {
            sockJSHandler.handle(routingContext);
        } finally {
            CurrentInstance.set(RoutingContext.class, null);
        }
    }

    private void callWithUi(final PushEvent event, final PushEventCallback callback) {

        SockJSSocket socket = event.socket;
        RoutingContext routingContext = event.routingContext;
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
                    callback.run(event, ui);
                }
            } catch (final IOException e) {
                callErrorHandler(session, e);
            } catch (final Exception e) {
                SystemMessages msg = service
                    .getSystemMessages(ServletPortletHelper.findLocale(null,
                        null, vaadinRequest), vaadinRequest);


                /* TODO: verify */
                SockJSSocket errorSocket = getOpenedPushConnection(socket, ui);
                sendNotificationAndDisconnect(errorSocket,
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

    private SockJSSocket getOpenedPushConnection(SockJSSocket socket, UI ui) {
        SockJSSocket errorSocket = socket;
        if (ui != null && ui.getPushConnection() != null) {
            // We MUST use the opened push connection if there is one.
            // Otherwise we will write the response to the wrong request
            // when using streaming (the client -> server request
            // instead of the opened push channel)
            errorSocket = ((SockJSPushConnection) ui.getPushConnection()).getSocket();
        }
        return errorSocket;
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
     * Checks whether a given push id matches the session's push id.
     *
     * @param session       the vaadin session for which the check should be done
     * @param requestPushId the push id provided in the request
     * @return {@code true} if the id is valid, {@code false} otherwise
     */
    private static boolean isPushIdValid(VaadinSession session, String requestPushId) {

        String sessionPushId = session.getPushId();
        return requestPushId != null && requestPushId.equals(sessionPushId);
    }

    /**
     * Tries to send a critical notification to the client and close the
     * connection. Does nothing if the connection is already closed.
     */
    private static void sendNotificationAndDisconnect(
        SockJSSocket socket, String notificationJson) {
        try {
            socket.write(Buffer.buffer(notificationJson, "UTF-8"));
            socket.end();
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
        void run(PushEvent event, UI ui) throws IOException;
    }

    private static class PushEvent {
        private final Buffer message;
        private final RoutingContext routingContext;
        private final SockJSSocket socket;

        PushEvent(SockJSSocket socket, RoutingContext routingContext, Buffer message) {
            this.message = message;
            this.routingContext = routingContext;
            this.socket = socket;
        }

        public SockJSSocket socket() {
            return socket;
        }

        Optional<Buffer> message() {
            return Optional.ofNullable(message);
        }
    }


}
