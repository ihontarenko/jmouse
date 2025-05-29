package org.jmouse.web.servlet;

import java.util.ArrayList;
import java.util.List;

public class HandlerExecutionChain {

    private final Object                   handler;
    private final List<HandlerInterceptor> interceptors = new ArrayList<>();

    public HandlerExecutionChain(Object handler) {
        this.handler = handler;
    }

    public Object getHandler() {
        return handler;
    }

    public List<HandlerInterceptor> getInterceptors() {
        return interceptors;
    }

}
