package org.jmouse.mvc;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.mvc.context.*;
import org.jmouse.web.WebLauncher;
import org.jmouse.web.initializer.ServletContextRegistrations;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

import java.util.Collection;
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
        ApplicationFactory<WebBeanContext> factory = new WebApplicationFactory();
        WebBeanContext                     context = factory.createRootContext();

        context.addInitializer(new BeanScanAnnotatedContextInitializer());
        context.addInitializer(new ApplicationContextBeansScanner());
        context.addInitializer(new StartupApplicationContextInitializer(context.getEnvironment()));
        context.addInitializer(new WebMvcControllersInitializer());
        context.addInitializer(new WebMvcInfrastructureInitializer());

        context.addBaseClasses(applicationClasses);
        context.refresh();

        createWebServer(context).start();

        return context;
    }

    @Override
    public WebServer createWebServer(WebBeanContext context) {
        List<WebApplicationInitializer> registrationBeans
                = WebBeanContext.getBeansOfType(WebApplicationInitializer.class, context);

        WebServerFactory factory = context.getBean(WebServerFactory.class);

        return factory.createWebServer(registrationBeans.toArray(WebApplicationInitializer[]::new));
    }

}
