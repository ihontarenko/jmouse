package org.jmouse.web.mvc.result;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jmouse.core.Priority;
import org.jmouse.web.http.RequestContext;

@Priority(1000)
public class StringValueReturnValueHandler implements ReturnValueHandler {

    @Override
    public void handleReturnValue(HandlerResult.ReturnValue result, RequestContext requestContext) {
        String returnValue = (String) result.value();
        HttpServletResponse servletResponse = requestContext.response();

        try {
            servletResponse.getWriter().write(returnValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(HandlerResult.ReturnValue result) {
        return result.value() instanceof String;
    }
}
