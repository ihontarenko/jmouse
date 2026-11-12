package org.jmouse.web.mvc;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.beans.WarmupEagerBeansContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.FeatureSelectorBeanContextInitializer;
import org.jmouse.web.WebApplicationFactory;
import org.jmouse.web.WebLauncher;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.initializer.context.StartupApplicationContextInitializer;
import org.jmouse.web.mvc.context.WebMvcControllersInitializer;
import org.jmouse.web.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;

/**
 * 🚀 Default implementation of {@link WebLauncher} for bootstrapping
 * a full web MVC application.
 *
 * <p>Responsible for:</p>
 * <ul>
 *   <li>Creating the sourceRoot {@link WebBeanContext}</li>
 *   <li>Registering core initializers (beans, MVC, infrastructure)</li>
 *   <li>Warming up critical components</li>
 *   <li>Creating and starting the {@link WebServer}</li>
 * </ul>
 */
public class WebApplicationLauncher implements WebLauncher<WebBeanContext> {

    /**
     * 📦 Application entry classes (annotated configs, bootstraps, etc.).
     */
    private final Class<?>[] applicationClasses;
    private Consumer<WebBeanContext> contextCustomizer = context -> {};

    /**
     * 🏗️ Create launcher with given application base classes.
     *
     * @param applicationClasses annotated application entry points
     */
    public WebApplicationLauncher(Class<?>... applicationClasses) {
        this.applicationClasses = applicationClasses.clone();
    }

    /** Customizes discovered definitions before eager initialization and server creation. */
    public WebApplicationLauncher configureContext(Consumer<WebBeanContext> customizer) {
        this.contextCustomizer = Objects.requireNonNull(customizer, "customizer");
        return this;
    }

    /**
     * ▶️ Launch the application and return the initialized {@link WebBeanContext}.
     *
     * <ul>
     *   <li>Builds sourceRoot context via {@link WebApplicationFactory}</li>
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
        context.addInitializer(new ApplicationContextBeansScanner());
        context.addInitializer(new WebMvcControllersInitializer());
        context.addInitializer(new WebMvcInfrastructureInitializer());
        context.addInitializer(new FeatureSelectorBeanContextInitializer());

        context.addBaseClasses(applicationClasses);
        context.refresh();

        contextCustomizer.accept(context);
        context.addInitializer(new WarmupEagerBeansContextInitializer());
        context.addInitializer(new StartupApplicationContextInitializer(context.getEnvironment()));
        context.refresh();
        warmup(context);

        WebServer server = createWebServer(context);
        try {
            context.registerBean(WebServer.class, server);
            server.start();
        } catch (RuntimeException | Error failure) {
            try {
                server.close();
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }

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
        List<WebApplicationInitializer> registrationBeans = WebBeanContext.getBeansOfType(
            WebApplicationInitializer.class,
            context
        );

        WebServerFactory factory = context.getBean(WebServerFactory.class);

        return factory.createWebServer(registrationBeans.toArray(WebApplicationInitializer[]::new));
    }
}
