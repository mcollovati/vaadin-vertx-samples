package com.github.mcollovati.vertxvaadin.fusion.data.endpoint;

import com.github.mcollovati.vertx.vaadin.VertxVaadin;
import com.vaadin.flow.server.Version;
import com.vaadin.fusion.Endpoint;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Nonnull;
import io.vertx.core.impl.launcher.commands.VersionCommand;

@AnonymousAllowed
@Endpoint
public class InfoEndpoint {

    @Nonnull
    public AppInfo info() {
        return AppInfo.INSTANCE;
    }

    public static class AppInfo {

        public static final AppInfo INSTANCE = new AppInfo();
        public final String vaadinVersion = Version.getFullVersion();
        public final String vertxVersion = VersionCommand.getVersion();
        public final String vertxVaadinVersion = VertxVaadin.getVersion();

        private AppInfo() {
        }
    }
}
