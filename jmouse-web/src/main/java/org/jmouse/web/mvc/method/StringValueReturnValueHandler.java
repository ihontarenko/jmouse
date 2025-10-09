package org.jmouse.web.mvc.method;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.http.RequestContext;
import org.jmouse.core.Priority;

import java.io.IOException;

@Priority(1000)
public class StringValueReturnValueHandler extends AbstractReturnValueHandler {

    @Override
    protected void doReturnValueHandle(MVCResult result, RequestContext requestContext) {
        String              returnValue     = (String) result.getReturnValue();
        HttpServletResponse servletResponse = requestContext.response();

        try {
            servletResponse.getWriter().write(returnValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(MVCResult result) {
        return result.getReturnValue() instanceof String;
    }

}
