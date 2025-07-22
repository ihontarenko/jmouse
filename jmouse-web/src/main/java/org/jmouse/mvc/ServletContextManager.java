package org.jmouse.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.BeanForWebContext;
import org.jmouse.mvc.context.WebControllersInitializer;
import org.jmouse.mvc.context.WebInfrastructureInitializer;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🧩 Manages creation and registration of servlet-based dispatcher contexts.
 * <p>
 * Used to create per-servlet {@link WebBeanContext} with isolated configuration,
 * infrastructure, and controllers.
 * </p>
 *
 * <pre>{@code
 * ServletContextManager    manager = new ServletContextManager(rootContext);
 * WebBeanContext           context = manager.createServletDispatcherContext("main", Controllers.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Bean
@PrimaryBean
@BeanForWebContext
public class ServletContextManager {

    private final WebBeanContext context;

    /**
     * 🔧 Constructs a manager with a shared root context.
     *
     * @param context the global root context for all web modules
     */
    @BeanConstructor
    public ServletContextManager(WebBeanContext context) {
        this.context = context;
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
    public WebBeanContext createServletDispatcherContext(String name, Class<?>[] basePackages) {
        Class<?>[] applicationClasses = context.getBean(Class[].class, WebBeanContext.DEFAULT_APPLICATION_CLASSES_BEAN);

        ApplicationFactory<WebBeanContext> factory           = context.getBean(ApplicationFactory.class);
        WebBeanContext                     dispatcherContext = factory.createContext(name, context, applicationClasses);

        dispatcherContext.addInitializer(new WebControllersInitializer(applicationClasses));
        dispatcherContext.addInitializer(new WebInfrastructureInitializer(applicationClasses));
        dispatcherContext.addInitializer(new ApplicationContextBeansScanner(applicationClasses));

        dispatcherContext.refresh();

        return dispatcherContext;
    }
}
