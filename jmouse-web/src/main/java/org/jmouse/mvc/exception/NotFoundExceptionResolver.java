package org.jmouse.mvc.exception;

import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpStatus;

public class NotFoundExceptionResolver extends AbstractExceptionResolver {

    @Override
    protected void doInitialize(WebBeanContext context) { }

    @Override
    protected InvocationOutcome doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        InvocationOutcome outcome = new Outcome(exception);
        outcome.setHttpStatus(HttpStatus.NOT_FOUND);
        return outcome;
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return exception instanceof NotFoundException;
    }

}
