package org.jmouse.web.initializer.application;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.servlet.WebBeanContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for configuring the {@link ServletContext} with the {@link WebBeanContextListener}.
 * Extends {@link AbstractWebApplicationInitializer} to include additional setup logic.
 */
@Priority(Integer.MIN_VALUE)
public class jMouseWebApplicationInitializer extends AbstractWebApplicationInitializer {

    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(jMouseWebApplicationInitializer.class);

    private final WebBeanContext context;

    @BeanConstructor
    public jMouseWebApplicationInitializer(WebBeanContext context) {
        this.context = context;
    }

    /**
     * Configures the {@link ServletContext} by adding the {@link WebBeanContextListener}.
     *
     * @param servletContext the {@link ServletContext} to configure.
     * @throws ServletException if any servlet-related error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);

        for (WebApplicationInitializer registration : new ServletContextRegistrations()
                .getRegistrationBeanInitializers(context)) {
            registration.onStartup(servletContext);
        }
    }

}
