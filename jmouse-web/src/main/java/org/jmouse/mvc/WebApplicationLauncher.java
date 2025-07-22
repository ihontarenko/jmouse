package org.jmouse.mvc;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.mvc.context.*;
import org.jmouse.web.WebLauncher;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

import java.util.List;

public class WebApplicationLauncher implements WebLauncher<WebBeanContext> {

    private final Class<?>[] applicationClasses;

    public WebApplicationLauncher(Class<?>... applicationClasses) {
        this.applicationClasses = applicationClasses;
    }

    /**
     * Launches the application and returns the initialized {@link WebBeanContext}.
     *
     * @param arguments optional command-line arguments
     * @return the fully initialized {@link WebBeanContext} instance
     */
    @Override
    public WebBeanContext launch(String... arguments) {
        ApplicationFactory<WebBeanContext> applicationFactory = new WebApplicationFactory();
        WebBeanContext                     rootContext        = applicationFactory.createRootContext();

        configureContextInitializers(rootContext);

        rootContext.addBaseClasses(applicationClasses);
        rootContext.refresh();

        createWebServer(rootContext).start();

        return rootContext;
    }

    public void configureContextInitializers(WebBeanContext rootContext) {
        rootContext.addInitializer(new BeanScanAnnotatedContextInitializer());
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(rootContext.getEnvironment()));
        rootContext.addInitializer(new ApplicationContextBeansScanner());
    }

    @Override
    public WebServer createWebServer(WebBeanContext rootContext) {
        List<WebApplicationInitializer> registrationBeans
                = WebBeanContext.getBeansOfType(WebApplicationInitializer.class, rootContext);

        WebServerFactory factory = rootContext.getBean(WebServerFactory.class);

        return factory.createWebServer(registrationBeans.toArray(WebApplicationInitializer[]::new));
    }

}
