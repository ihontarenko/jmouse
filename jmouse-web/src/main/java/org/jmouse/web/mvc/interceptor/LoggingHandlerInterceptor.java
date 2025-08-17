package org.jmouse.web.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.HandlerInterceptor;
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
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, MVCResult result) {
        LOGGER.info("Post-Handle: URI: {}; Handler: {};", request.getRequestURI(), handler.getClass());
    }

}
