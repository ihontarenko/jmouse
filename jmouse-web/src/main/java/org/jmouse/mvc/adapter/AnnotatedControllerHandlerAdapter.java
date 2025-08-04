package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.InvocationTargetException;

public class AnnotatedControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(
            HttpServletRequest request, HttpServletResponse response, MappedHandler handler, MvcContainer container) {
        HandlerMethod handlerMethod = (HandlerMethod) handler.handler();
        Object        returnValue;
        try {
            Object bean = handlerMethod.getBean();
            returnValue = handlerMethod.getMethod().invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        // No-op
    }

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof HandlerMethod;
    }
}
