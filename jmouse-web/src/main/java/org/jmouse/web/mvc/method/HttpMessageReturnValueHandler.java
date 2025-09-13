package org.jmouse.web.mvc.method;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.mvc.HandlerMapping;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.method.converter.ServletHttpOutputMessage;
import org.jmouse.web.mvc.method.converter.HttpMessageConverter;
import org.jmouse.web.mvc.method.converter.HttpOutputMessage;
import org.jmouse.web.mvc.method.converter.MessageConverterManager;
import org.jmouse.web.mvc.method.converter.UnsuitableException;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.core.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.negotiation.MediaTypeManager;

import java.io.IOException;
import java.util.*;

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

    private MessageConverterManager converterManager;
    private MediaTypeManager        mediaTypeManager;
    /**
     * Initializes this handler by retrieving the {@link MessageConverterManager} from the application context.
     *
     * @param context the current {@link WebBeanContext}
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        converterManager = context.getBean(MessageConverterManager.class);
        mediaTypeManager = context.getBean(MediaTypeManager.class);
    }

    /**
     * Handles the given controller method return value by selecting an appropriate
     * {@link HttpMessageConverter} and writing the converted output to the HTTP response.
     *
     * @param result        the invocation result, containing the return value and method metadata
     * @param requestContext the current request context
     * @throws UnsuitableException if no compatible message converter is found
     */
    @Override
    protected void doReturnValueHandle(MVCResult result, RequestContext requestContext) {
        Object              returnValue     = result.getReturnValue();
        HttpServletResponse servletResponse = requestContext.response();
        MediaType           contentType     = null;

        // requested media-types
        List<MediaType> acceptableTypes = getAcceptableMediaTypes(requestContext.request());
        // applicable media-types
        List<MediaType> producibleTypes = getProducibleMediaTypes(requestContext.request());
        // compatible media-types
        List<MediaType> compatibleTypes = getCompatibleMediaTypes(acceptableTypes, producibleTypes);

        HttpMessageConverter<Object> messageConverter = null;

        // Select the first matching converter
        if (!compatibleTypes.isEmpty()) {
            for (MediaType mediaType : compatibleTypes) {
                messageConverter = converterManager.getMessageConverter(returnValue, mediaType.toString());
                if (messageConverter != null) {
                    contentType = mediaType;
                    break;
                }
            }
        }

        if (messageConverter == null) {
            throw new UnsuitableException("No suitable converter found for: " + acceptableTypes);
        }

        HttpOutputMessage httpMessage   = new ServletHttpOutputMessage(servletResponse);
        Headers           outputHeaders = httpMessage.getHeaders();

        outputHeaders.setContentType(contentType);
        outputHeaders.setHeader(HttpHeader.X_JMOUSE_DEBUG, "DEBUG MESSAGE!");

        try {
            messageConverter.write(returnValue, result.getReturnType().getReturnType(), httpMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ✅ Checks whether this handler supports the given return type.
     *
     * @param result the invocation result to check
     * @return {@code true} if return type is not {@code void} and not {@code null}
     */
    @Override
    public boolean supportsReturnType(MVCResult result) {
        return result.getReturnType() != null && !result.getReturnType().getReturnType().equals(void.class);
    }

    /**
     * 📦 Resolve producible media types for the current request.
     *
     * <ul>
     *   <li>Reads {@link HandlerMapping#ROUTE_PRODUCIBLE_ATTRIBUTE} if present.</li>
     *   <li>Falls back to all supported media types of {@code converterManager}.</li>
     * </ul>
     *
     * @param request current HTTP request
     * @return immutable list of producible media types
     */
    @SuppressWarnings("unchecked")
    protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
        List<MediaType> result = new ArrayList<>();

        if (request.getAttribute(HandlerMapping.ROUTE_PRODUCIBLE_ATTRIBUTE) instanceof Set<?> producible) {
            result = (List<MediaType>) List.copyOf(producible);
        }

        if (result.isEmpty()) {
            result = converterManager.getSupportedMediaTypes();
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * 📡 Resolve acceptable media types declared by the client.
     *
     * <p>Delegates to {@code mediaTypeManager} to parse the request.</p>
     *
     * @param request current HTTP request
     * @return immutable list of acceptable media types
     */
    protected List<MediaType> getAcceptableMediaTypes(HttpServletRequest request) {
        return mediaTypeManager.lookupOnRequest(request);
    }

    /**
     * 🔗 Compute compatible media types between what client accepts
     * and what the server can produce.
     *
     * <ul>
     *   <li>Checks compatibility for each pair (acceptable vs. producible).</li>
     *   <li>Copies quality factor (q) and charset where applicable.</li>
     *   <li>Chooses the more specific type when resolving overlap.</li>
     * </ul>
     *
     * @param acceptableTypes  list of client-accepted types
     * @param producibleTypes  list of server-producible types
     * @return immutable list of compatible media types
     */
    protected List<MediaType> getCompatibleMediaTypes(List<MediaType> acceptableTypes, List<MediaType> producibleTypes) {
        Set<MediaType> compatibleTypes = new HashSet<>();

        for (MediaType requestedType : acceptableTypes) {
            for (MediaType producibleType : producibleTypes) {
                if (producibleType.compatible(requestedType)) {
                    producibleType = producibleType.copyQFactor(requestedType);
                    producibleType = producibleType.copyCharset(requestedType);
                    MediaType compatibleType = new MediaType(MediaType.getMoreSpecific(producibleType, requestedType));
                    compatibleTypes.add(compatibleType);
                }
            }
        }

        return List.copyOf(compatibleTypes);
    }

}
