package org.jmouse.web.servlet.registration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Common interface for programmatic registration of web components
 * (servlets, filters, listeners) into a ServletContext.
 */
public interface WebServerRegistrationBean {

    /**
     * Register the underlying component with the given ServletContext.
     * @param servletContext the target ServletContext
     * @throws ServletException on registration failure
     */
    void register(ServletContext servletContext) throws ServletException;

    /**
     * Order of this registration; lower values have higher priority.
     */
    int getOrder();

    /**
     * Set the registration order.
     */
    void setOrder(int order);

    /**
     * Is current registration is enabled.
     */
    boolean isEnabled();

    /**
     * Short name of component.
     */
    String getName();

}
