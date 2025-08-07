package org.jmouse.mvc.exception;

import org.jmouse.mvc.AbstractExceptionResolver;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.NotFoundException;
import org.jmouse.mvc.RequestContext;
import org.jmouse.web.context.WebBeanContext;

public class NotFoundExceptionResolver extends AbstractExceptionResolver<NotFoundException> {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doExceptionResolve(
            RequestContext requestContext, InvocationOutcome outcome, NotFoundException exception) {

    }

    @Override
    public boolean supportsException(Throwable exception) {
        return exception instanceof NotFoundException;
    }

}
