package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

final public class HandlerExecution {

    private final List<HandlerInterceptor> interceptors = new ArrayList<>();
    private Object handler;

    public HandlerExecution(Object handler) {
        this.handler = handler;
    }

    public List<HandlerInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }

    public void addInterceptor(HandlerInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    public Object getHandler() {
        return handler;
    }

    public void setHandler(Object handler) {
        this.handler = handler;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        boolean successful = true;

        for (HandlerInterceptor interceptor : getInterceptors()) {
            if (!interceptor.preHandle(request, response, getHandler())) {
                successful = false;
                break;
            }
        }

        return successful;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, HandlerResponse result) {
        for (HandlerInterceptor interceptor : getInterceptors()) {
            interceptor.postHandle(request, response, getHandler(), result);
        }
    }

}
