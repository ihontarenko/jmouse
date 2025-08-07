package app.api;

import org.jmouse.mvc.AbstractExceptionResolver;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.RequestContext;
import org.jmouse.web.context.WebBeanContext;

public class ApiExceptionResolver extends AbstractExceptionResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {
        System.out.println(context);
    }

    @Override
    protected InvocationOutcome doExceptionResolve(RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        System.out.println(requestContext.request());

        return null;
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return exception instanceof IllegalStateException;
    }

}
