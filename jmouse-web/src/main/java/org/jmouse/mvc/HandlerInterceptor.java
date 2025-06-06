package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerInterceptor {

    default boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        return false;
    }

    default void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, HandlerResponse result) {
    }

}
