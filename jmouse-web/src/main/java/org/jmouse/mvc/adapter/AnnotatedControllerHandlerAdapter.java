package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class AnnotatedControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            MappedHandler mappedHandler,
            InvocationResult result
    ) {
        HandlerMethod handlerMethod = (HandlerMethod) mappedHandler.handler();
        MappingResult mappingResult = MappingResult.of(mappedHandler.match(), mappedHandler.route());

        Object        returnValue;

        try {
            Object       bean      = handlerMethod.getBean();
            Object[]     arguments = {};
            List<Object> resolved  = new ArrayList<>();

            for (MethodParameter parameter : handlerMethod.getParameters()) {
                for (ArgumentResolver argumentResolver : getArgumentResolvers()) {
                    if (argumentResolver.supportsParameter(parameter)) {
                        resolved.add(argumentResolver.resolveArgument(parameter, mappingResult));
                    }
                }
            }

            if (!resolved.isEmpty()) {
                arguments = resolved.toArray();
            }

            returnValue = handlerMethod.getMethod().invoke(bean, arguments);
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
