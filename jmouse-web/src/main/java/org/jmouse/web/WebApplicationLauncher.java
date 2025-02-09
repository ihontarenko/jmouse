package org.jmouse.web;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.bind.*;
import org.jmouse.core.env.Environment;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.ServletWebApplicationInitializer;
import org.jmouse.web.initializer.application.WebBeanContextServletInitializer;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerConfig;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.WebServers;

import java.util.ArrayList;
import java.util.Map;

public class WebApplicationLauncher {

    private final Class<?>[]                         baseClasses;
    private final ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
    private  Map<WebServers, WebServerConfig> webserver;
    private Binder binder;

    public WebApplicationLauncher(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
    }

    public WebBeanContext launch() {
        Environment environment = applicationFactory.createDefaultEnvironment();

        ScannerBeanContextInitializer scannerBeanContextInitializer = new ScannerBeanContextInitializer();

        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ServletWebApplicationInitializer.class, rootTypes)));
        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ApplicationConfigurer.class, rootTypes)));

        WebBeanContext rootWebBeanContext = applicationFactory.createContext(WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, baseClasses);
        rootWebBeanContext.addInitializer(scannerBeanContextInitializer);
        rootWebBeanContext.addInitializer(new StartupRootApplicationContextInitializer(environment));
        rootWebBeanContext.refresh();

        binder = Binder.withDataSource(environment);

        WebBeanContext webBeanContext = applicationFactory.createContext(WebBeanContext.DEFAULT_WEB_CONTEXT_NAME, rootWebBeanContext, baseClasses);

        Bind.with(binder).to("jmouse.web", this);

        Bind.with(binder).to("jmouse.web.server.default", WebServers.class).ifPresent(webServers -> {
            System.out.println("Default WebServers Configuration: " + getWebserver().get(webServers));
        });

        // web server part
        rootWebBeanContext.getBeans(ServletWebApplicationInitializer.class);
        WebServerFactory factory   = rootWebBeanContext.getBean(WebServerFactory.class);
        WebServer        webServer = factory.getWebServer(new WebBeanContextServletInitializer(webBeanContext));
        webServer.start();

        return rootWebBeanContext;
    }

    public Map<WebServers, WebServerConfig> getWebserver() {
        return webserver;
    }

    @PropertyPath("server")
    public void setWebserver(Map<WebServers, WebServerConfig> webserver) {
        this.webserver = webserver;
    }

}
