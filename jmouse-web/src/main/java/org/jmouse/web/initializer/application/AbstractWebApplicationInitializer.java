package org.jmouse.web.initializer.application;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.web.initializer.WebApplicationInitializer;

/**
 * An abstract implementation of the {@link WebApplicationInitializer} interface.
 * Provides a default empty implementation of the {@code onStartup} method for subclasses to override.
 */
public abstract class AbstractWebApplicationInitializer implements WebApplicationInitializer {

    /**
     * Called during the startup phase of the servlet context.
     * Subclasses can override this method to provide specific initialization logic.
     *
     * @param servletContext the {@link ServletContext} being initialized.
     * @throws ServletException if any error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }
}
