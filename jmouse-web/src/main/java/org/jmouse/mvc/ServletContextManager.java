package org.jmouse.mvc;

import org.jmouse.context.ApplicationFactory;
import org.jmouse.mvc.context.WebControllersInitializer;
import org.jmouse.mvc.context.WebInfrastructureInitializer;
import org.jmouse.web.context.WebBeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🧩 Manages creation and registration of servlet-based dispatcher contexts.
 * <p>
 * Used to create per-servlet {@link WebBeanContext} with isolated configuration,
 * infrastructure, and controllers.
 * </p>
 *
 * <pre>{@code
 * ServletContextManager manager = new ServletContextManager(rootContext);
 * WebBeanContext context = manager.createDispatcherContext("main", MyControllers.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class ServletContextManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletContextManager.class);

    private final WebBeanContext rootContext;

    /**
     * 🔧 Constructs a manager with a shared root context.
     *
     * @param rootContext the global root context for all web modules
     */
    public ServletContextManager(WebBeanContext rootContext) {
        this.rootContext = rootContext;
    }

    /**
     * 🛠️ Creates a new {@link WebBeanContext} for a servlet dispatcher.
     *
     * <p>
     * Registers infrastructure and controller initializers, then refreshes the context.
     * </p>
     *
     * @param name         the dispatcher context name
     * @param basePackages packages to scan for controllers and components
     * @return the fully initialized dispatcher context
     */
    @SuppressWarnings("unchecked")
    public WebBeanContext createDispatcherContext(String name, Class<?>[] basePackages) {
        LOGGER.info("⚙️ Creating dispatcher context: '{}'", name);

        ApplicationFactory<WebBeanContext> factory = rootContext.getBean(ApplicationFactory.class);
        WebBeanContext dispatcherContext = factory.createContext(name, rootContext);

        dispatcherContext.addInitializer(new WebInfrastructureInitializer(basePackages));
        dispatcherContext.addInitializer(new WebControllersInitializer(basePackages));

        LOGGER.info("🚀 Initializing dispatcher context: '{}'", name);
        dispatcherContext.refresh();
        LOGGER.info("✅ Dispatcher context '{}' ready", name);

        return dispatcherContext;
    }
}
