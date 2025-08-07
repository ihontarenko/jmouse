package org.jmouse.mvc.exception;

import org.jmouse.mvc.AbstractExceptionResolver;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.RequestContext;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashSet;
import java.util.Set;

public class ExceptionHandlerExceptionResolver extends AbstractExceptionResolver {

    private final Set<Class<? extends Throwable>> supportedExceptions;

    public ExceptionHandlerExceptionResolver() {
        this.supportedExceptions = new HashSet<>();
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return supportedExceptions.contains(exception.getClass());
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        for (String beanName : context.getBeanNames(Object.class)) {
            if (context.isLocalBean(beanName)) {

            }
        }
    }

    @Override
    protected InvocationOutcome doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        return null;
    }

}
