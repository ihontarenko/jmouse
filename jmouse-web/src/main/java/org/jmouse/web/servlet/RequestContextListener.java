package org.jmouse.web.servlet;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanScope;

import java.util.function.Consumer;

/**
 * 🧩 Servlet listener that manages {@link RequestAttributesHolder} for each HTTP request.
 *
 * <p>It initializes per-thread request-scoped context at the start of the request
 * and removes it after the request is completed.
 *
 * <p>Useful for enabling global access to request metadata (path, headers, query, route)
 * without passing {@link HttpServletRequest} through the entire call chain.
 *
 * <p><b>Registered via:</b> <br>
 * In {@code web.xml} or via {@code @WebListener} or programmatically via ServletContext.
 *
 * @author Ivan Hontarenko
 * @since jMouse Web 1.0
 */
public class RequestContextListener implements ServletRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestContextListener.class);

    /**
     * Initializes thread-local request context at the beginning of a request.
     * <p>Called automatically by the Servlet container.
     *
     * @param event the request event fired when a new request is initialized
     */
    @Override
    public void requestInitialized(ServletRequestEvent event) {
        if (event.getServletRequest() instanceof HttpServletRequest servletRequest) {

            // Attach objects to thread-local context (if present)
            attach(servletRequest, RequestAttributesHolder::setRequestPath,
                   RequestPath.ofRequest(servletRequest), RequestPath.REQUEST_PATH_ATTRIBUTE);
            attach(servletRequest, RequestAttributesHolder::setRequestRoute,
                   RequestRoute.ofRequest(servletRequest), RequestRoute.REQUEST_ROUTE_ATTRIBUTE);
            attach(servletRequest, RequestAttributesHolder::setQueryParameters,
                   RequestAttributesHolder.getRequestRoute().queryParameters(), QueryParameters.QUERY_PARAMETERS_ATTRIBUTE);
            attach(servletRequest, RequestAttributesHolder::setRequestHeaders,
                   RequestHeaders.ofRequest(servletRequest), RequestHeaders.REQUEST_HEADERS_ATTRIBUTE);

            // Set core request attributes (used as fallback context container)
            RequestAttributesHolder.setRequestAttributes(
                    RequestAttributes.ofRequest(BeanScope.REQUEST, servletRequest)
            );

            LOGGER.debug("✅ Initialized request context for [{}]", servletRequest.getRequestURI());
        }
    }

    /**
     * Removes all thread-local request context on request destruction.
     *
     * @param event the request event fired when a request is about to be destroyed
     */
    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        remove(RequestAttributesHolder::removeRequestAttributes, RequestAttributes.class);
        remove(RequestAttributesHolder::removeRequestPath, RequestPath.class);
        remove(RequestAttributesHolder::removeQueryParameters, QueryParameters.class);
        remove(RequestAttributesHolder::removeRequestRoute, RequestRoute.class);
        remove(RequestAttributesHolder::removeRequestHeaders, RequestHeaders.class);

        LOGGER.debug("🧹 Cleaned up request context for [{}]",
                ((HttpServletRequest) event.getServletRequest()).getRequestURI());
    }

    /**
     * Helper method to attach an object to both the request and the thread-local holder.
     */
    private <T> void attach(HttpServletRequest request, Consumer<T> attacher, T object, String attributeKey) {
        if (object != null) {
            request.setAttribute(attributeKey, object);
            attacher.accept(object);
            LOGGER.debug("🔗 Attached [{}] to request with key [{}]", object.getClass().getSimpleName(), attributeKey);
        }
    }

    /**
     * Helper method to log and remove an object from thread-local holder.
     */
    private <T> void remove(Runnable remover, Class<T> type) {
        remover.run();
        LOGGER.debug("❌ Removed thread-local [{}]", type.getSimpleName());
    }
}
