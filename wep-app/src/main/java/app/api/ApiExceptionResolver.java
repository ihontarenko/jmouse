package app.api;

import org.jmouse.mvc.AbstractExceptionResolver;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.RequestContext;
import org.jmouse.web.context.WebBeanContext;

public class ApiExceptionResolver extends AbstractExceptionResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {
        System.out.println(context);
    }

    @Override
    protected void doExceptionResolve(RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        System.out.println(requestContext.request());
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return exception instanceof IllegalStateException;
    }

}
