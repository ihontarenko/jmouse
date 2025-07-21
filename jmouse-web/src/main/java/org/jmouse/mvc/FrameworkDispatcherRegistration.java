package org.jmouse.mvc;

import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

/**
 * 🌀 Registers the core {@link FrameworkDispatcher} servlet.
 * <p>
 * Integrates with {@link WebBeanContext} for full DI support and request handling.
 * </p>
 *
 * <pre>{@code
 * new FrameworkDispatcherRegistration(webContext);
 * }</pre>
 *
 * @see FrameworkDispatcher
 * @see WebBeanContext
 * @see ServletRegistrationBean
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public class FrameworkDispatcherRegistration extends ServletRegistrationBean<FrameworkDispatcher> {

    /**
     * ⚙️ Constructs dispatcher registration with default name.
     *
     * @param context the web application context
     */
    public FrameworkDispatcherRegistration(WebBeanContext context) {
        this(FrameworkDispatcher.DEFAULT_DISPATCHER, context);
    }

    /**
     * ⚙️ Constructs dispatcher registration with a custom servlet name.
     *
     * @param servletName custom dispatcher servlet name
     * @param context     the web application context
     */
    public FrameworkDispatcherRegistration(String servletName, WebBeanContext context) {
        super(servletName, new FrameworkDispatcher(context));
    }
}
