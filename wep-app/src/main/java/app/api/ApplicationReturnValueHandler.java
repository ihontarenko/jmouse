package app.api;

import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.method.AbstractReturnValueHandler;

public class ApplicationReturnValueHandler extends AbstractReturnValueHandler {
    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {

    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return false;
    }
}
