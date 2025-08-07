package org.jmouse.mvc.adapter.return_value;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;
import java.util.Map;

@Priority(500)
public class JsonReturnValueHandler extends AbstractReturnValueHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        @SuppressWarnings("unchecked")
        Map<String, Object> returnValue     = (Map<String, Object>) result.getReturnValue();
        HttpServletResponse servletResponse = requestContext.response();

        try {
            servletResponse.setContentType(MediaType.APPLICATION_JSON.toString());
            objectMapper.writeValue(servletResponse.getWriter(), returnValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() instanceof Map<?, ?>;
    }

}
