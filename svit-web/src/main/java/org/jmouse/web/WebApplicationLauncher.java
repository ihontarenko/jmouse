package org.jmouse.web;

import svit.beans.ScannerBeanContextInitializer;
import org.jmouse.core.env.Environment;
import org.jmouse.core.observer.EventManager;
import org.jmouse.core.observer.EventManagerFactory;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.ServletWebApplicationInitializer;
import org.jmouse.web.initializer.application.WebBeanContextServletInitializer;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

import java.util.ArrayList;

public class WebApplicationLauncher {

    private final EventManager                       eventManager;
    private final Class<?>[]                         baseClasses;
    private final ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();

    public WebApplicationLauncher(Class<?>... baseClasses) {
        this.baseClasses = baseClasses;
        this.eventManager = EventManagerFactory.create(baseClasses);
    }

    public WebBeanContext launch() {
        Environment environment = applicationFactory.createDefaultEnvironment();

        ScannerBeanContextInitializer scannerBeanContextInitializer = new ScannerBeanContextInitializer();

        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ServletWebApplicationInitializer.class, rootTypes)));
        scannerBeanContextInitializer.addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ApplicationConfigurer.class, rootTypes)));

        WebBeanContext rootWebBeanContext = applicationFactory.createContext("WEB-ROOT", baseClasses);
        rootWebBeanContext.addInitializer(scannerBeanContextInitializer);
        rootWebBeanContext.addInitializer(new StartupRootApplicationContextInitializer(environment));
        rootWebBeanContext.refresh();

        WebBeanContext webBeanContext = applicationFactory.createContext(
                "WEB-APPLICATION", rootWebBeanContext, baseClasses);

        // web server part
        rootWebBeanContext.getBeans(ServletWebApplicationInitializer.class);
        WebServerFactory factory   = rootWebBeanContext.getBean(WebServerFactory.class);
        WebServer        webServer = factory.getWebServer(new WebBeanContextServletInitializer(webBeanContext));
        webServer.start();

        return rootWebBeanContext;
    }

}
