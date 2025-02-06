package org.jmouse.web.initializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Interface for initializing a web application.
 * Implementations provide custom startup logic for configuring the {@link ServletContext}.
 */
public interface ServletWebApplicationInitializer {

    /**
     * Called during the startup phase of the servlet context.
     * Implementations should provide initialization logic, such as configuring listeners, filters, or servlets.
     *
     * @param servletContext the {@link ServletContext} being initialized.
     * @throws ServletException if any error occurs during initialization.
     */
    void onStartup(ServletContext servletContext) throws ServletException;
}
