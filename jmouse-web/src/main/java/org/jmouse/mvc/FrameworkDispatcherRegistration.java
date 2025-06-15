package org.jmouse.mvc;

import org.jmouse.beans.annotation.Ignore;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.ServletRegistrationBean;

/**
 * 🌀 Registers the core {@link FrameworkDispatcher} servlet into the web application context.
 * <p>
 * This registration bean ensures that the dispatcher is available to handle incoming requests
 * and integrates it with the {@link WebBeanContext} for dependency resolution.
 * </p>
 *
 * <p>This class is marked with {@code @Ignore} so it can be excluded from component scanning.</p>
 *
 * @see FrameworkDispatcher
 * @see WebBeanContext
 * @see ServletRegistrationBean
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
@Ignore
public class FrameworkDispatcherRegistration extends ServletRegistrationBean<FrameworkDispatcher> {

    /**
     * Constructs and registers the default {@link FrameworkDispatcher} servlet
     * using the provided {@link WebBeanContext}.
     *
     * @param context the web bean context used for handler resolution and dependency injection
     */
    public FrameworkDispatcherRegistration(WebBeanContext context) {
        super(FrameworkDispatcher.DEFAULT_DISPATCHER, new FrameworkDispatcher(context));
    }

}
