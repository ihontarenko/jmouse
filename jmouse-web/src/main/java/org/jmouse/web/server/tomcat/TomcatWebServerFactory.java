package org.jmouse.web.server.tomcat;

import org.apache.catalina.startup.Tomcat;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.web.initializer.ServletWebApplicationInitializer;
import org.jmouse.web.servlet.initializer.jMouseServletContainerInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

public class TomcatWebServerFactory implements WebServerFactory, BeanContextAware {

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
    public WebServer getWebServer(ServletWebApplicationInitializer... initializers) {
        WebServer                    webServer  = new TomcatWebServer();
        // todo: need to add some properties reader or environment mechanism
        WebServer.Configurer<Tomcat> configurer = new TomcatWebServerConfigurer(8899,
                new jMouseServletContainerInitializer(initializers));

        if (webServer.server() != null) {
            webServer.configure(configurer);
        }

        return webServer;
    }

}
