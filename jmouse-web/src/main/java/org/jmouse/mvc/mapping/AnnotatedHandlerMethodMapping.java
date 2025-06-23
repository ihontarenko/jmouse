package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.AbstractHandlerMapping;
import org.jmouse.mvc.HandlerInterceptor;

import java.util.List;

public class AnnotatedHandlerMethodMapping extends AbstractHandlerMapping {
    /**
     * 🎯 Resolve handler object from request.
     *
     * @param request HTTP request
     * @return handler object or {@code null} if not matched
     */
    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        return null;
    }

    /**
     * 🧩 List of interceptors for current mapping.
     *
     * @return list of interceptors (can be empty)
     */
    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return List.of();
    }
}
