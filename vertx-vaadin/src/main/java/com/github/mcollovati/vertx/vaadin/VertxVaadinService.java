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

import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.ServletPortletHelper;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.ServletBootstrapHandler;
import com.vaadin.server.communication.ServletUIInitHandler;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Created by marco on 16/07/16.
 */
public class VertxVaadinService extends VaadinService {

    private static final Logger logger = LoggerFactory.getLogger(VertxVaadinService.class);

    private final VaadinVerticle vaadinVerticle;

    public VertxVaadinService(VaadinVerticle vaadinVerticle, DefaultDeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.vaadinVerticle = vaadinVerticle;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
        throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        handlers.add(0, new ServletBootstrapHandler());
        handlers.add(new ServletUIInitHandler());
        if (isAtmosphereAvailable()) {
            handlers.add((RequestHandler) (session, request, response) -> {
                if (!ServletPortletHelper.isPushRequest(request)) {
                    return false;
                }
                if (request instanceof VertxVaadinRequest) {
                    ((VertxVaadinRequest) request).getRoutingContext().next();
                }
                return true;
            });
        }
        return handlers;
    }

    @Override
    public boolean ensurePushAvailable() {
        return true;
    }

    @Override
    public String getStaticFileLocation(VaadinRequest request) {
        String staticFileLocation;
        // if property is defined in configurations, use that
        staticFileLocation = getDeploymentConfiguration().getResourcesPath();
        if (staticFileLocation != null) {
            return staticFileLocation;
        }

        VertxVaadinRequest vertxRequest = (VertxVaadinRequest) request;
        String requestedPath = vertxRequest.getRequest().path()
            .substring(
                Optional.ofNullable(vertxRequest.getRoutingContext().mountPoint())
                    .map(String::length).orElse(0)
            );
        return VaadinServletService.getCancelingRelativePath(requestedPath);
    }

    @Override
    public String getConfiguredWidgetset(VaadinRequest request) {
        return getDeploymentConfiguration().getWidgetset(VaadinServlet.DEFAULT_WIDGETSET);
    }

    @Override
    public String getConfiguredTheme(VaadinRequest request) {
        return ValoTheme.THEME_NAME;
    }

    @Override
    public boolean isStandalone(VaadinRequest request) {
        return true;
    }

    @Override
    public String getMimeType(String resourceName) {
        return null;
    }

    // TODO: from system property?
    @Override
    public File getBaseDirectory() {
        return new File(".");
    }


    // From VaadinServletService
    @Override
    protected boolean requestCanCreateSession(VaadinRequest request) {
        if (ServletUIInitHandler.isUIInitRequest(request)) {
            // This is the first request if you are embedding by writing the
            // embedding code yourself
            return true;
        } else if (isOtherRequest(request)) {
            /*
             * I.e URIs that are not RPC calls or static (theme) files.
             */
            return true;
        }

        return false;
    }

    private boolean isOtherRequest(VaadinRequest request) {
        // TODO This should be refactored in some way. It should not be
        // necessary to check all these types.
        return (!ServletPortletHelper.isAppRequest(request)
            && !ServletUIInitHandler.isUIInitRequest(request)
            && !ServletPortletHelper.isFileUploadRequest(request)
            && !ServletPortletHelper.isHeartbeatRequest(request)
            && !ServletPortletHelper.isPublishedFileRequest(request)
            && !ServletPortletHelper.isUIDLRequest(request) && !ServletPortletHelper
            .isPushRequest(request));
    }

    @Override
    public String getServiceName() {
        return vaadinVerticle.deploymentID();
    }

    @Override
    public InputStream getThemeResourceAsStream(UI uI, String themeName, String resource) {
        return null;
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request, Class<? extends UI> uiClass) {
        return vaadinVerticle.deploymentID() + Math.abs(vaadinVerticle.deploymentID().hashCode());
    }


    public Vertx getVertx() {
        return vaadinVerticle.getVertx();
    }

    public VaadinVerticle getVerticle() {
        return vaadinVerticle;
    }

}
