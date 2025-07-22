package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.util.Sorter;
import org.jmouse.web.request.RequestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 🧭 Base class for handler mappings.
 *
 * Provides common logic for resolving a handler and attaching sorted interceptors.
 * Subclasses must implement handler lookup and interceptor list retrieval.
 *
 * Example usage:
 * <pre>{@code
 * public class MyHandlerMapping extends AbstractHandlerMapping {
 *     protected Object doGetHandler(HttpServletRequest req) { ... }
 *     protected List<HandlerInterceptor> getHandlerInterceptors() { ... }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public abstract class AbstractHandlerMapping implements HandlerMapping {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractHandlerMapping.class);

    /**
     * 🔎 Resolves the handler for the current request and attaches interceptors.
     *
     * @param request current HTTP request
     * @return container with handler and interceptors, or {@code null} if no handler found
     */
    @Override
    public Handler getHandler(HttpServletRequest request) {
        Object handler = doGetHandler(request);

        if (handler == null) {
            return null;
        }

        Handler                  container    = new Handler(handler);
        List<HandlerInterceptor> interceptors = getHandlerInterceptors();

        if (interceptors != null && !interceptors.isEmpty()) {
            Sorter.sort(interceptors);
            interceptors.forEach(container::addInterceptor);
        }

        return container;
    }

    public String getMappingPath(HttpServletRequest request) {
        String mappingPath = request.getRequestURI();

        if (request.getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE) instanceof RequestPath requestPath) {
            mappingPath = requestPath.requestPath();
        }

        return mappingPath;
    }

    /**
     * 🎯 Resolve handler object from request.
     *
     * @param request HTTP request
     * @return handler object or {@code null} if not matched
     */
    protected abstract Object doGetHandler(HttpServletRequest request);

    /**
     * 🧩 List of interceptors for current mapping.
     *
     * @return list of interceptors (can be empty)
     */
    protected abstract List<HandlerInterceptor> getHandlerInterceptors();
}
