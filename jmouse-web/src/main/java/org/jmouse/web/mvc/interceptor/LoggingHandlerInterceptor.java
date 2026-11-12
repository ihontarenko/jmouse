package org.jmouse.web.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.HandlerInterceptor;
import org.jmouse.web.mvc.result.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHandlerInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHandlerInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LOGGER.info("Pre-Handle: URI: {}; Handler: {};", request.getRequestURI(), handler.getClass());
        return true;
    }

    @Override
    public HandlerResult postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        HandlerResult result
    ) {
        LOGGER.info("Post-Handle: URI: {}; Handler: {};", request.getRequestURI(), handler.getClass());
        return result;
    }
}
