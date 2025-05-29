package org.jmouse.web;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.bind.*;
import org.jmouse.core.env.Environment;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.ServletWebApplicationInitializer;
import org.jmouse.web.initializer.application.WebBeanContextServletInitializer;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.*;

import java.util.ArrayList;
import java.util.Map;

public class WebApplicationLauncher {

    public static final String JMOUSE_WEB_ROOT_PROPERTIES = "jmouse.web";
    private final Class<?>[] baseClasses;
    private final ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
    private       Map<WebServers, WebServerConfig>   webserver;

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

        WebBeanContext rootContext = applicationFactory.createContext(
                WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, baseClasses);

        rootContext.addInitializer(scannerBeanContextInitializer);
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));

        rootContext.refresh();

        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        WebServerConfigHolder webServerConfigHolder = new WebServerConfigHolder();

        Bind.with(rootContext.getEnvironmentBinder()).to(
                WebServerFactory.WEB_SERVER_CONFIGURATION_PATH, webServerConfigHolder);

        // web server part
        WebServerFactory factory   = rootContext.getBean(WebServerFactory.class);
        WebServer        webServer = factory.getWebServer(new WebBeanContextServletInitializer(rootContext));

        webServer.start();

        return rootContext;
    }

    public Map<WebServers, WebServerConfig> getWebserver() {
        return webserver;
    }

    @BindName("server")
    public void setWebserver(Map<WebServers, WebServerConfig> webserver) {
        this.webserver = webserver;
    }

}
