package com.vaadin.server.communication;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;
import org.atmosphere.cpr.AtmosphereResource;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by marco on 18/07/16.
 */
public interface ExposeVaadin {

    static String getUINotFoundErrorJSON(VaadinService service, VaadinRequest vaadinRequest) {
        return UidlRequestHandler.getUINotFoundErrorJSON(service, vaadinRequest);
    }

    static AtmosphereResource resourceFromPushConnection(UI ui) {
        return ((AtmospherePushConnection) ui.getPushConnection()).getResource();
    }

    static Reader readMessageFromPushConnection(AtmospherePushConnection pushConnection, Reader reader) throws IOException {
        return pushConnection.receiveMessage(reader);
    }
}
