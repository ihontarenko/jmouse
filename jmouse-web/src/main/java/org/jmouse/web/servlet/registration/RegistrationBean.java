package org.jmouse.web.servlet.registration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common interface for programmatic registration of web components
 * (servlets, filters, listeners) into a ServletContext.
 */
public interface RegistrationBean {

    Logger LOGGER = LoggerFactory.getLogger(RegistrationBean.class);

    /**
     * Register the underlying component with the given ServletContext.
     * @param servletContext the target ServletContext
     * @throws ServletException on registration failure
     */
    void register(ServletContext servletContext) throws ServletException;

    /**
     * Order of this registration; lower values have higher priority.
     */
    default int getOrder() {
        return Reflections.getAnnotationValue(getClass(), Priority.class, Priority::value);
    }

    /**
     * Set order of this registration; lower values have higher priority.
     */
    default void setOrder(int order) {}

    /**
     * Is current registration is enabled.
     */
    boolean isEnabled();

    /**
     * Short name of component.
     */
    String getName();

}
