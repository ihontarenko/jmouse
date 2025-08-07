package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;

@Priority(1000)
public class TextReturnValueHandler extends AbstractReturnValueHandler {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        String returnValue     = (String) result.getReturnValue();
        HttpServletResponse servletResponse = requestContext.response();

        try {
            servletResponse.getWriter().write("TEXT:" + returnValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() instanceof String;
    }

}
