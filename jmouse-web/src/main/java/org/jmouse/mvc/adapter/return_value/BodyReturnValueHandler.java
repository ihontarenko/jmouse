package org.jmouse.mvc.adapter.return_value;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.WebHttpServletResponse;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.mvc.converter.HttpMessageConverter;
import org.jmouse.mvc.converter.MessageConverterManager;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.RequestAttributesHolder;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Priority(500)
public class BodyReturnValueHandler extends AbstractReturnValueHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MessageConverterManager converterManager;

    @Override
    protected void doInitialize(WebBeanContext context) {
        converterManager = context.getBean(MessageConverterManager.class);
    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        @SuppressWarnings("unchecked")
        Map<String, Object>          returnValue      = (Map<String, Object>) result.getReturnValue();
        HttpServletResponse          servletResponse  = requestContext.response();
        HttpMessageConverter<Object> messageConverter = null;
        Headers                      headers          = RequestAttributesHolder.getRequestHeaders().headers();

        List<MediaType> acceptance = headers.getAccept();
        acceptance.sort(Comparator.comparingDouble(MediaType::getQFactor));
        acceptance = acceptance.reversed();

        for (MediaType acceptType : acceptance) {
            messageConverter = converterManager.getMessageConverter(returnValue, acceptType.toString());
            if (messageConverter != null) {
                break;
            }
        }

        try {
            messageConverter.write(returnValue, Object.class, new WebHttpServletResponse(servletResponse));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() instanceof Map<?, ?> && !outcome.getReturnParameter().getReturnType().equals(void.class);
    }

}
