package org.jmouse.web;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.AbstractApplicationFactory;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.conversion.ContextConversion;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.environment.Environment;
import org.jmouse.core.events.EventManager;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.ResourceLoader;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.web.mvc.context.WebMvcControllersInitializer;
import org.jmouse.web.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.core.StandardPlaceholderReplacer;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupApplicationContextInitializer;

import java.util.HashSet;
import java.util.Set;

/**
 * 🕸️ Factory class for creating and configuring {@link WebBeanContext} instances
 * in a web-based jMouse application.
 *
 * <p>Provides methods to construct sourceRoot and child web contexts, inject default beans,
 * and configure MVC infrastructure.</p>
 *
 * <pre>{@code
 * WebApplicationFactory factory = new WebApplicationFactory();
 * WebBeanContext rootContext = factory.createRootContext();
 * WebBeanContext childContext = factory.createApplicationContext("web-ctx", rootContext, MyApp.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WebApplicationFactory extends AbstractApplicationFactory<WebBeanContext> {

    public static final String ROUTE_REPLACER_BEAN_NAME   = "routeReplacer";
    public static final String DEFAULT_REPLACER_BEAN_NAME = "defaultReplacer";

    /**
     * Creates a new {@link WebBeanContext} with no parent.
     *
     * @param contextId   the identifier for the context.
     * @param baseClasses the base classes to scan for components.
     * @return a new {@link WebBeanContext} instance.
     */
    @Override
    public WebBeanContext createContext(String contextId, Class<?>... baseClasses) {
        return createContext(contextId, null, baseClasses);
    }

    /**
     * Creates a new {@link WebBeanContext} with the given parent context.
     *
     * @param contextId   the identifier for the context.
     * @param rootContext optional parent context (typically the sourceRoot).
     * @param classes     the base classes to scan.
     * @return a new {@link WebBeanContext} instance.
     */
    @Override
    public WebBeanContext createContext(String contextId, WebBeanContext rootContext, Class<?>... classes) {
        Set<Class<?>> baseClasses = new HashSet<>(Set.of(classes));

        WebBeanContext context = createContextInstance(contextId, baseClasses.toArray(Class<?>[]::new));

        context.setParentContext(rootContext);
        context.refresh(); // Triggers pre-initialization lifecycle

        return context;
    }

    /**
     * Creates and configures the sourceRoot web application context.
     * Registers core beans such as {@link Environment}, {@link Conversion}, {@link ResourceLoader}, etc.
     *
     * @return a fully initialized sourceRoot {@link WebBeanContext}.
     */
    @Override
    public WebBeanContext createRootContext() {
        WebBeanContext context = createContext(WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME, getClass());

        context.registerBean(Environment.class, createDefaultEnvironment());
        context.registerBean(ApplicationFactory.class, this);
        context.registerBean(Conversion.class, new ContextConversion());
        context.registerBean(ResourceLoader.class, new CompositeResourceLoader());
        context.registerBean(ExpressionLanguage.class, new ExpressionLanguage());
        context.registerBean(DEFAULT_REPLACER_BEAN_NAME, new StandardPlaceholderReplacer());
        context.registerBean(ROUTE_REPLACER_BEAN_NAME, new StandardPlaceholderReplacer("{", "}", ":"));
        context.registerBean(EventManager.class, context.getEventManager());

        return context;
    }

    /**
     * Creates and initializes a full-featured web application context.
     * Adds default initializers for component scanning and MVC infrastructure.
     *
     * @param contextId   unique context ID.
     * @param rootContext the sourceRoot context (parent).
     * @param classes     application base classes.
     * @return initialized web application context.
     */
    @Override
    public WebBeanContext createApplicationContext(String contextId, WebBeanContext rootContext, Class<?>... classes) {
        WebBeanContext context = createContext(contextId, rootContext, classes);

        // Add common application initializers
        context.addInitializer(new BeanScanAnnotatedContextInitializer());
        context.addInitializer(new ApplicationContextBeansScanner());
        context.addInitializer(new StartupApplicationContextInitializer(rootContext.getEnvironment()));
        context.addInitializer(new WebMvcControllersInitializer());

        context.refresh(); // Re-run initialization after initializers added

        // Initializes MVC-specific infrastructure
        new WebMvcInfrastructureInitializer().initialize(context);

        return context;
    }

    /**
     * Creates a raw {@link WebBeanContext} instance with base classes and sets its ID.
     *
     * @param contextId   the identifier to assign to the context.
     * @param baseClasses classes to scan.
     * @return a new uninitialized context.
     */
    private WebBeanContext createContextInstance(String contextId, Class<?>... baseClasses) {
        WebBeanContext context = new WebApplicationBeanContext(baseClasses);
        context.setContextId(contextId);
        return context;
    }
}
