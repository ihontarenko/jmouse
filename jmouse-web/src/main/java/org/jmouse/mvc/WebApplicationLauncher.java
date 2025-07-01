package org.jmouse.mvc;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.core.env.Environment;
import org.jmouse.mvc.context.*;
import org.jmouse.web.WebLauncher;
import org.jmouse.web.jMouseWebRoot;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.initializer.WebApplicationInitializerProvider;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.servlet.registration.RegistrationBean;

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
        Environment                        environment        = applicationFactory.createDefaultEnvironment();
        WebBeanContext                     rootContext        = applicationFactory.createContext(
                WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, jMouseWebRoot.class, jMouseWebMvcRoot.class);

        rootContext.registerBean(Environment.class, environment);

        rootContext.addInitializer(new BeanScanAnnotatedContextInitializer());
        rootContext.addInitializer(new ApplicationContextBeansScanner());

        rootContext.addInitializer(new CoreFrameworkInitializer());
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));

        rootContext.refresh();

        // attach ApplicationFactory object
        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        // web server part
        createWebServer(rootContext).start();

        return rootContext;
    }

    @Override
    public WebServer createWebServer(WebBeanContext rootContext) {
        List<WebApplicationInitializer> initializers = new WebApplicationInitializerProvider(
                rootContext).getExcluding(WebApplicationInitializer.class);

        // initializers run instantiations of registaration. but should be for per servlet
        WebServerFactory factory = rootContext.getBean(WebServerFactory.class);

        return factory.getWebServer(initializers.toArray(WebApplicationInitializer[]::new));
    }

}
