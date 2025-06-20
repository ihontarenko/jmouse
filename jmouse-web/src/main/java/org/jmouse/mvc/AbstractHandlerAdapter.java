package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

abstract public class AbstractHandlerAdapter implements HandlerAdapter {

    @Override
    public HandlerResult handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HandlerResult handlerResponse = null;

        Object returnValue = doHandle(request, response, handler);

        return handlerResponse;
    }

    abstract protected Object doHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

}
