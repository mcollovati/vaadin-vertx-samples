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

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.router.Route;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.startup.CustomElementRegistryInitializer;
import com.vaadin.server.startup.RouteRegistryInitializer;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by marco on 16/07/16.
 */
public class VaadinVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(VaadinVerticle.class);

    private HttpServer httpServer;
    private VertxVaadinService vaadinService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        log.info("Starting vaadin verticle " + getClass().getName());

        VaadinVerticleConfiguration vaadinVerticleConfiguration = getClass().getAnnotation(VaadinVerticleConfiguration.class);

        JsonObject vaadinConfig = new JsonObject();
        vaadinConfig.put("serviceName", this.deploymentID());
        vaadinConfig.put("mountPoint", Optional.ofNullable(vaadinVerticleConfiguration)
            .map(VaadinVerticleConfiguration::mountPoint).orElse("/")
        );
        Optional.ofNullable(vaadinVerticleConfiguration).map(VaadinVerticleConfiguration::basePackages)
            .map(pkgs -> new JsonArray(Arrays.asList(pkgs)))
            .ifPresent(pkgs -> vaadinConfig.put("flowBasePackages", pkgs));
        readUiFromEnclosingClass(vaadinConfig);
        readConfigurationAnnotation(vaadinConfig);
        vaadinConfig.mergeIn(config().getJsonObject("vaadin", new JsonObject()));

        String mountPoint = vaadinConfig.getString("mountPoint");
        vaadinConfig.put(ApplicationConstants.CONTEXT_ROOT_URL, mountPoint);

        initFlow(vaadinConfig);

        VertxVaadin vertxVaadin = createVertxVaadin(vaadinConfig);
        vaadinService = vertxVaadin.vaadinService();

        HttpServerOptions serverOptions = new HttpServerOptions().setCompressionSupported(true);
        httpServer = vertx.createHttpServer(serverOptions);

        Router router = Router.router(vertx);
        router.mountSubRouter(mountPoint, vertxVaadin.router());
        httpServer.websocketHandler(vertxVaadin.webSocketHandler());
        httpServer.requestHandler(router::accept).listen(config().getInteger("httpPort", 8080));


        serviceInitialized(vaadinService, router);


        log.info("Started vaadin verticle " + getClass().getName());
        startFuture.complete();
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping vaadin verticle " + getClass().getName());
        try {
            vaadinService.destroy();
            log.info("Vaadin service destroyed");
        } catch (Exception ex) {
            log.error("Error during Vaadin service destroy", ex);
        }
        httpServer.close();
        log.info("Stopped vaadin verticle " + getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private void initFlow(JsonObject vaadinConfig) throws ServletException {
        List<String> pkgs = vaadinConfig.getJsonArray("flowBasePackages", new JsonArray()).getList();
        Set<Class<?>> routesClassSet = new LinkedHashSet<>();
        Set<Class<?>> customElementsClassSet = new LinkedHashSet<>();
        new FastClasspathScanner(pkgs.toArray(new String[pkgs.size()]))
            .matchClassesWithAnnotation(Route.class, routesClassSet::add)
            .matchClassesWithAnnotation(Tag.class, customElementsClassSet::add)
            .scan();
        StubServletContext servletContext = new StubServletContext(context);
        new RouteRegistryInitializer().onStartup(nullSetIfEmpty(routesClassSet), servletContext);
        new CustomElementRegistryInitializer().onStartup(nullSetIfEmpty(customElementsClassSet), servletContext);
    }

    private Set<Class<?>> nullSetIfEmpty(Set<Class<?>> classSet) {
        return classSet.isEmpty() ? null : classSet;
    }

    protected VertxVaadin createVertxVaadin(JsonObject vaadinConfig) {
        return VertxVaadin.create(vertx, vaadinConfig);
    }

    /*private static class RouteRegistryInitializer extends AbstractRouteRegistryInitializer {

    }*/

    protected void serviceInitialized(VertxVaadinService service, Router router) {
    }

    // From VaadinServlet
    private void readUiFromEnclosingClass(JsonObject vaadinConfig) {
        Class<?> enclosingClass = getClass().getEnclosingClass();

        if (enclosingClass != null && UI.class.isAssignableFrom(enclosingClass)) {
            vaadinConfig.put(VaadinSession.UI_PARAMETER, enclosingClass.getName());
        }
    }

    // From VaadinServlet
    private void readConfigurationAnnotation(JsonObject vaadinConfig) {

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

                    vaadinConfig.put(name.value(), stringValue);
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

class StubServletContext implements ServletContext {

    private final Context context;


    public StubServletContext(Context context) {
        this.context = context;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        String relativePath = path;
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        FileSystem fileSystem = context.owner().fileSystem();
        FileProps props = fileSystem.propsBlocking(relativePath);
        if (props != null && props.isDirectory()) {
            System.out.println("============= " + path + " Is directory");
            return new LinkedHashSet<>(
                fileSystem.readDirBlocking(path).stream()
                    .map(p -> {
                        String n = Paths.get(p).getFileName().toString();
                        if (fileSystem.propsBlocking(p).isDirectory()) {
                            n += "/";
                        }
                        return path + n;
                    })
                    .collect(Collectors.toList())
            );
        }
        System.out.println("============= " + path + " Is not directory. " + props);
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        String relativePath = path;
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        FileSystem fileSystem = context.owner().fileSystem();
        FileProps props = fileSystem.propsBlocking(relativePath);
        if (props != null && !props.isDirectory()) {
            Buffer buffer = fileSystem.readFileBlocking(relativePath);
            return new ByteArrayInputStream(buffer.getBytes());
        }
        return null;

    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return null;
    }

    @Override
    public Enumeration<String> getServletNames() {
        return null;
    }

    @Override
    public void log(String msg) {

    }

    @Override
    public void log(Exception exception, String msg) {

    }

    @Override
    public void log(String message, Throwable throwable) {

    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return context.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public void setAttribute(String name, Object object) {
        context.put(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        context.remove(name);
    }

    @Override
    public String getServletContextName() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }
}