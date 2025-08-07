package org.jmouse.mvc.exception;

import org.jmouse.mvc.AbstractExceptionResolver;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.RequestContext;
import org.jmouse.web.context.WebBeanContext;

public class ExceptionHandlerExceptionResolver extends AbstractExceptionResolver {

    @Override
    public boolean supportsException(Throwable exception) {
        return false;
    }

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {

    }

}
