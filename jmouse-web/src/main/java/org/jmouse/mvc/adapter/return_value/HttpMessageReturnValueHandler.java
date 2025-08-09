package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.WebHttpServletResponse;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.mvc.converter.HttpMessageConverter;
import org.jmouse.mvc.converter.HttpOutputMessage;
import org.jmouse.mvc.converter.MessageConverterManager;
import org.jmouse.mvc.converter.UnsuitableException;
import org.jmouse.mvc.mapping.annotation.Mapping;
import org.jmouse.util.Priority;
import org.jmouse.util.Streamable;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.RequestAttributesHolder;
import org.jmouse.web.request.http.HttpHeader;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 📡 Handles return values by converting them to HTTP responses using message converters.
 *
 * <p>Selects an appropriate {@link HttpMessageConverter} based on the request's Accept header
 * or the controller's {@link Mapping#produces()} declaration.</p>
 *
 * <p>Writes the converted output to the {@link HttpServletResponse} with proper headers.</p>
 *
 * @author Ivan Hontarenko
 */
@Priority(500)
public class HttpMessageReturnValueHandler extends AbstractReturnValueHandler {

    private MessageConverterManager converterManager;

    @Override
    protected void doInitialize(WebBeanContext context) {
        converterManager = context.getBean(MessageConverterManager.class);
    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome outcome, RequestContext requestContext) {
        Object                       returnValue      = outcome.getReturnValue();
        HttpServletResponse          servletResponse  = requestContext.response();
        HttpMessageConverter<Object> messageConverter = null;
        Headers                      incomingHeaders  = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType                    contentType      = null;

        // Sort Accept header by descending quality factor
        List<MediaType> acceptance = incomingHeaders.getAccept();
        acceptance.sort(Comparator.comparingDouble(MediaType::getQFactor));
        acceptance = acceptance.reversed();

        // If Accept header is empty, try to get produces info from @Mapping annotation
        if (acceptance.isEmpty()) {
            Optional<MergedAnnotation> optional = AnnotationRepository.ofAnnotatedElement(outcome.getReturnParameter()
                    .getAnnotatedElement()).get(Mapping.class);
            if (optional.isPresent()) {
                Mapping mapping = optional.get().synthesize();
                if (mapping.produces() != null && mapping.produces().length > 0) {
                    acceptance = Streamable.of(mapping.produces())
                            .map(MimeParser::parseMimeType)
                            .map(MediaType::new)
                            .toList();
                }
            }
        }

        // Find a suitable message converter for the accepted media types
        if (!acceptance.isEmpty()) {
            for (MediaType acceptType : acceptance) {
                messageConverter = converterManager.getMessageConverter(returnValue, acceptType.toString());
                if (messageConverter != null) {
                    contentType = acceptType;
                    break;
                }
            }
        }

        if (messageConverter == null) {
            throw new UnsuitableException("No suitable converter found for: " + acceptance);
        }

        HttpOutputMessage httpMessage   = new WebHttpServletResponse(servletResponse);
        Headers           outputHeaders = httpMessage.getHeaders();

        outputHeaders.setContentType(contentType);
        outputHeaders.setHeader(HttpHeader.X_JMOUSE_DEBUG, "debug message!");

        try {
            messageConverter.write(returnValue, Object.class, httpMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() != null && !outcome.getReturnParameter().getReturnType().equals(void.class);
    }
}
