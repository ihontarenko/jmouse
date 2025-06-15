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
    public WebServer getWebServer(WebApplicationInitializer... initializers) {
        WebServer                    webServer  = new TomcatWebServer();
        WebServerConfig              config     = configuration.getWebServerConfig(WebServers.TOMCAT);
        WebServer.Configurer<Tomcat> configurer = new TomcatWebServerConfigurer(
                config.port(), new jMouseServletContainerInitializer(initializers));

        if (webServer.server() != null) {
            webServer.configure(configurer);
        }

        return webServer;
    }

}
