package org.jmouse.web.initializer.application;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.RequestContextListener;
import org.jmouse.web.servlet.WebBeanContextListener;

import java.util.EventListener;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Initializer for configuring the {@link ServletContext} with the {@link WebBeanContextListener}.
 * Extends {@link AbstractApplicationInitializer} to include additional setup logic.
 */
@Priority(Integer.MIN_VALUE)
public class WebBeanContextServletInitializer extends AbstractApplicationInitializer {

    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebBeanContextServletInitializer.class);

    private final WebBeanContext webBeanContext;

    @BeanConstructor
    public WebBeanContextServletInitializer(WebBeanContext webBeanContext) {
        this.webBeanContext = webBeanContext;
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

        registerServletContextListeners(servletContext);
        registerRequestContextListeners(servletContext);
    }

    private void registerServletContextListeners(ServletContext servletContext) {
        // Add the WebBeanContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new WebBeanContextListener(webBeanContext));
    }

    private void registerRequestContextListeners(ServletContext servletContext) {
        // Add the RequestContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new RequestContextListener());
    }

    private void registerServletContextListener(ServletContext servletContext, EventListener listener) {
        if (listener != null) {
            servletContext.addListener(listener);
            LOGGER.info("Event listener '{}' registered to ServletContext", getShortName(listener.getClass()));
        }
    }
}
