package org.jmouse.web.servlet;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanScope;
import org.jmouse.web.request.RequestAttributes;
import org.jmouse.web.request.RequestAttributesHolder;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A listener for creating and cleaning up {@link RequestAttributes}
 * during the lifecycle of an HTTP servlet request.
 * <br>
 * This helps maintain request-specific data within the current thread,
 * which is especially useful for frameworks or utilities that need
 * to access the current HTTP request state without passing it around directly.
 */
public class RequestContextListener implements ServletRequestListener {

    /**
     * The logger instance for this class.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(RequestContextListener.class);

    /**
     * Initializes request attributes when an HTTP request is received.
     * If the incoming request is an instance of {@link HttpServletRequest},
     * sets the current thread's {@link RequestAttributes} using
     * {@link RequestAttributesHolder#setRequestAttributes(RequestAttributes)}.
     *
     * @param event the {@link ServletRequestEvent} containing the request.
     */
    @Override
    public void requestInitialized(ServletRequestEvent event) {
        if (event.getServletRequest() instanceof HttpServletRequest servletRequest) {
            LOGGER.info("Update {}[{}] structured to '{}' holder", getShortName(servletRequest.getClass().getName()),
                        servletRequest.getRequestURI(), RequestAttributesHolder.class.getName());
            RequestAttributesHolder.setRequestAttributes(
                    RequestAttributes.of(BeanScope.REQUEST, servletRequest)
            );

            RequestAttributesHolder.getRequestAttributes().setAttribute("attr1", "val");
        }
    }

    /**
     * Cleans up the request attributes when the HTTP request is destroyed.
     * Removes the current thread's {@link RequestAttributes} using
     * {@link RequestAttributesHolder#clearRequestAttributes()}.
     *
     * @param event the {@link ServletRequestEvent} containing the request
     */
    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        LOGGER.info("Remove request holder {}", RequestAttributesHolder.class.getName());
        RequestAttributesHolder.clearRequestAttributes();
    }
}
