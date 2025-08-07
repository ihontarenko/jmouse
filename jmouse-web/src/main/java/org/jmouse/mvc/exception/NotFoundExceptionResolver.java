package org.jmouse.mvc.exception;

import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpHeader;
import org.jmouse.web.request.http.HttpStatus;

public class NotFoundExceptionResolver extends AbstractExceptionResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected InvocationOutcome doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        requestContext.response().setStatus(HttpStatus.NOT_FOUND.getCode());
        requestContext.response().setHeader(HttpHeader.X_TEXT.toString(), exception.getMessage());
        return null;
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return exception instanceof NotFoundException;
    }

}
