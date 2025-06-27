package org.jmouse.mvc;

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
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

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

        // ⚙️ Core configuration (WebServerFactory, BeanInstanceInitializer)
        rootContext.addInitializer(new CoreFrameworkInitializer());

        // ⚙️ Core configuration (BeanProperties, ApplicationConfigurer)
        rootContext.addInitializer(new CoreConfigurationInitializer());

        // ☀️ Custom startup initializer
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(environment));

        rootContext.refresh();

        // attach ApplicationFactory object
        rootContext.registerBean(ApplicationFactory.class, applicationFactory);

        // 🔵 2. DispatcherContext (child)
        WebBeanContext dispatcherContext = applicationFactory.createContext("dispatcher", rootContext);

        dispatcherContext.addInitializer(new WebInfrastructureInitializer(jMouseWebMvcRoot.class));
        dispatcherContext.addInitializer(new WebControllersInitializer(applicationClasses));
        dispatcherContext.refresh(); // 🟦 run initializing of local context

        // 🧩 Реєстрація DispatcherServlet
        registerDispatcherServlet(rootContext, dispatcherContext);

        // web server part
        createWebServer(rootContext).start();

        return rootContext;
    }

    @Override
    public WebServer createWebServer(WebBeanContext rootContext) {
        List<WebApplicationInitializer> initializers = new WebApplicationInitializerProvider(
                rootContext).getExcluding(RegistrationBean.class);

        WebServerFactory factory = rootContext.getBean(WebServerFactory.class);

        return factory.getWebServer(initializers.toArray(WebApplicationInitializer[]::new));
    }

    private void registerDispatcherServlet(WebBeanContext root, WebBeanContext dispatcher) {
        FrameworkDispatcherRegistration registration = new FrameworkDispatcherRegistration(dispatcher);

        registration.addMappings("/*");
        registration.setEnabled(true);
        registration.setLoadOnStartup(1);

        root.registerBean(ServletRegistrationBean.class, () -> registration);
    }

}
