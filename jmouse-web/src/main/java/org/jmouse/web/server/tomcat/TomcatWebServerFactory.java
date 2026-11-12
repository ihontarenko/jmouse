package org.jmouse.web.server.tomcat;

import org.apache.catalina.startup.Tomcat;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.annotation.Dependency;
import org.jmouse.web.factories.WebServerFactoryConfiguration;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerConfig;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.WebServers;
import org.jmouse.web.servlet.initializer.jMouseServletContainerInitializer;

public class TomcatWebServerFactory implements WebServerFactory, BeanContextAware {

    @Dependency
    private WebServerFactoryConfiguration.WebServerConfigHolder configuration;

    private BeanContext context;
    private final WebServerConfig explicitConfiguration;

    public TomcatWebServerFactory() {
        this.explicitConfiguration = null;
    }

    /** Programmatic factory configuration without a bean container. */
    public TomcatWebServerFactory(WebServerConfig configuration) {
        this.explicitConfiguration = java.util.Objects.requireNonNull(configuration, "configuration");
    }

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.context = context;
    }

    @Override
    public WebServer createWebServer(WebApplicationInitializer... initializers) {
        WebServer webServer = new TomcatWebServer();
        WebServerConfig config =
            explicitConfiguration != null
                ? explicitConfiguration
                : java.util.Objects.requireNonNull(
                      configuration,
                      "Web server configuration is not initialized"
                  ).getWebServerConfig(WebServers.TOMCAT);
        java.util.Objects.requireNonNull(config, "Tomcat server configuration is missing");
        WebServer.Configurer<Tomcat> configurer = new TomcatWebServerConfigurer(
            config,
            new jMouseServletContainerInitializer(initializers)
        );

        try {
            webServer.configure(configurer);
            return webServer;
        } catch (RuntimeException | Error failure) {
            try {
                webServer.close();
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }
}
