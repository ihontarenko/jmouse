package org.jmouse.web.mvc;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.beans.ProxyFactoryContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.web.WebLauncher;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupApplicationContextInitializer;
import org.jmouse.web.mvc.context.WebMvcControllersInitializer;
import org.jmouse.web.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

import java.util.List;

/**
 * 🚀 Default implementation of {@link WebLauncher} for bootstrapping
 * a full web MVC application.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>Creating the root {@link WebBeanContext}</li>
 *   <li>Registering core initializers (beans, MVC, infrastructure)</li>
 *   <li>Warming up critical components</li>
 *   <li>Creating and starting the {@link WebServer}</li>
 * </ul>
 */
public class WebApplicationLauncher implements WebLauncher<WebBeanContext> {

    /** 📦 Application entry classes (annotated configs, bootstraps, etc.). */
    private final Class<?>[] applicationClasses;

    /**
     * 🏗️ Create launcher with given application base classes.
     *
     * @param applicationClasses annotated application entry points
     */
    public WebApplicationLauncher(Class<?>... applicationClasses) {
        this.applicationClasses = applicationClasses;
    }

    /**
     * ▶️ Launch the application and return the initialized {@link WebBeanContext}.
     *
     * <ul>
     *   <li>Builds root context via {@link WebApplicationFactory}</li>
     *   <li>Registers context and MVC initializers</li>
     *   <li>Refreshes context and warms it up</li>
     *   <li>Creates and starts embedded {@link WebServer}</li>
     * </ul>
     *
     * @param arguments optional command-line arguments
     * @return fully initialized {@link WebBeanContext}
     */
    @Override
    public WebBeanContext launch(String... arguments) {
        ApplicationFactory<WebBeanContext> factory = new WebApplicationFactory();
        WebBeanContext context = factory.createRootContext();

        context.addInitializer(new BeanScanAnnotatedContextInitializer());
        context.addInitializer(new ProxyFactoryContextInitializer());
        context.addInitializer(new ApplicationContextBeansScanner());
        context.addInitializer(new StartupApplicationContextInitializer(context.getEnvironment()));
        context.addInitializer(new WebMvcControllersInitializer());
        context.addInitializer(new WebMvcInfrastructureInitializer());

        context.addBaseClasses(applicationClasses);
        context.refresh();

        warmup(context);

        createWebServer(context).start();

        return context;
    }

    /**
     * 🌐 Create a web server bound to the current {@link WebBeanContext}.
     *
     * @param context active web bean context
     * @return ready-to-start web server
     */
    @Override
    public WebServer createWebServer(WebBeanContext context) {
        List<WebApplicationInitializer> registrationBeans =
                WebBeanContext.getBeansOfType(WebApplicationInitializer.class, context);

        WebServerFactory factory = context.getBean(WebServerFactory.class);

        return factory.createWebServer(registrationBeans.toArray(WebApplicationInitializer[]::new));
    }

    /**
     * 🔥 Warm up critical MVC infrastructure (e.g. {@link HandlerDispatcher}).
     *
     * @param context active web bean context
     */
    @Override
    public void warmup(WebBeanContext context) {
        context.getBean(HandlerDispatcher.class);
    }
}
