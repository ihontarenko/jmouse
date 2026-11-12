package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.result.HandlerResult;

/** Request interception surrounds invocation; postHandle transforms an immutable outcome before rendering. */
public interface HandlerInterceptor {
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }

    /** Return the original result for observation-only interceptors, or a replacement to transform it. */
    default HandlerResult postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        HandlerResult result
    ) {
        return result;
    }
}
