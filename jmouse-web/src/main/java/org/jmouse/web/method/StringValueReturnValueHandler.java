package org.jmouse.web.method;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.util.Priority;

import java.io.IOException;

@Priority(1000)
public class StringValueReturnValueHandler extends AbstractReturnValueHandler {

    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        String              returnValue     = (String) result.getReturnValue();
        HttpServletResponse servletResponse = requestContext.response();

        try {
            servletResponse.getWriter().write(returnValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() instanceof String;
    }

}
