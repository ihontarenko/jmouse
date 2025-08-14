package org.jmouse.web.method;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.MimeParser;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.method.converter.WebHttpServletResponse;
import org.jmouse.web.method.converter.HttpMessageConverter;
import org.jmouse.web.method.converter.HttpOutputMessage;
import org.jmouse.web.method.converter.MessageConverterManager;
import org.jmouse.web.method.converter.UnsuitableException;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.core.Priority;
import org.jmouse.core.Streamable;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.http.HttpHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 📡 Handles controller return values by converting them into HTTP responses using {@link HttpMessageConverter}s.
 *
 * <p>Determines the appropriate {@link HttpMessageConverter} by checking:</p>
 * <ul>
 *   <li>The {@link Mapping#produces()} attribute on the handler method, if present</li>
 *   <li>The <code>Accept</code> header from the incoming request</li>
 * </ul>
 *
 * <p>Writes the converted output to the {@link HttpServletResponse} and applies the correct
 * <code>Content-Type</code> header. Also injects debug information into the
 * <code>X-JMOUSE-DEBUG</code> response header.</p>
 *
 * @author Ivan Hontarenko
 */
@Priority(500)
public class HttpMessageReturnValueHandler extends AbstractReturnValueHandler {

    /** The manager responsible for locating the appropriate {@link HttpMessageConverter}. */
    private MessageConverterManager converterManager;

    /**
     * Initializes this handler by retrieving the {@link MessageConverterManager} from the application context.
     *
     * @param context the current {@link WebBeanContext}
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        converterManager = context.getBean(MessageConverterManager.class);
    }

    /**
     * Handles the given controller method return value by selecting an appropriate
     * {@link HttpMessageConverter} and writing the converted output to the HTTP response.
     *
     * @param outcome        the invocation outcome, containing the return value and method metadata
     * @param requestContext the current request context
     * @throws UnsuitableException if no compatible message converter is found
     */
    @Override
    protected void doReturnValueHandle(InvocationOutcome outcome, RequestContext requestContext) {
        Object                       returnValue      = outcome.getReturnValue();
        HttpServletResponse          servletResponse  = requestContext.response();
        HttpMessageConverter<Object> messageConverter = null;
        Headers                      incomingHeaders  = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType                    contentType      = null;
        List<MediaType>              acceptance       = getProduces(outcome.getReturnParameter());

        // Fallback to Accept header if no explicit 'produces' declaration
        if (acceptance.isEmpty()) {
            acceptance = incomingHeaders.getAccept();
        }

        // Sort by quality factor (q)
        acceptance = new ArrayList<>(acceptance);
        acceptance.sort(Comparator.comparingDouble(MediaType::getQFactor));
        acceptance = acceptance.reversed();

        // Select the first matching converter
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
        outputHeaders.setHeader(HttpHeader.X_JMOUSE_DEBUG, "DEBUG MESSAGE!");

        try {
            messageConverter.write(returnValue, outcome.getReturnParameter().getReturnType(), httpMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether this handler supports the given return type.
     *
     * @param outcome the invocation outcome to check
     * @return {@code true} if the return value is non-null and not {@code void}, otherwise {@code false}
     */
    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() != null && !outcome.getReturnParameter().getReturnType().equals(void.class);
    }

    /**
     * Retrieves the list of media types declared via {@link Mapping#produces()} on the given method parameter.
     *
     * @param parameter the method parameter to inspect
     * @return a list of produced media types, or an empty list if none declared
     */
    protected List<MediaType> getProduces(MethodParameter parameter) {
        List<MediaType>            acceptance = new ArrayList<>();
        Optional<MergedAnnotation> annotation = AnnotationRepository
                .ofAnnotatedElement(parameter.getAnnotatedElement())
                .get(Mapping.class);

        if (annotation.isPresent()) {
            Mapping  mapping  = annotation.get().synthesize();
            String[] produces = mapping.produces();
            if (produces != null && produces.length > 0) {
                acceptance = Streamable.of(produces)
                        .map(MimeParser::parseMimeType)
                        .map(MediaType::new)
                        .toList();
            }
        }

        return acceptance;
    }
}
