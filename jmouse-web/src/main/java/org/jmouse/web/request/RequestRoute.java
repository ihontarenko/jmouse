package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.request.http.HttpMethod;

import java.util.Map;
import java.util.Set;

/**
 * 🛬 Represents an incoming HTTP request for routing and matching purposes.
 *
 * <p>Used to match against route conditions like path, method, headers or media types.</p>
 *
 * <pre>{@code
 * RequestRoute route = RequestRoute.ofRequest(httpServletRequest);
 * }</pre>
 *
 * @param requestPath path information from request (already parsed)
 * @param method HTTP method (GET, POST, etc.)
 * @param headers request headers (flattened view)
 * @param contentType value of "Content-Type" header, if present
 * @param accept set of accepted response types from "Accept" header
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RequestRoute(
        HttpMethod method,
        RequestPath requestPath,
        QueryParameters queryParameters,
        Headers headers,
        MediaType contentType,
        Set<MediaType> accept
) {

    public static final String REQUEST_ROUTE_ATTRIBUTE = RequestRoute.class.getName() + ".REQUEST_ROUTE";

    /**
     * 🏗️ Builds a {@code RequestRoute} from a {@link HttpServletRequest}.
     *
     * <p>Uses current {@link RequestAttributesHolder} to get parsed path.</p>
     *
     * @param request raw servlet request
     * @return route-compatible wrapper
     */
    public static RequestRoute ofRequest(HttpServletRequest request) {
        RequestPath    requestPath    = RequestAttributesHolder.getRequestPath();
        RequestHeaders requestHeaders = RequestHeaders.ofRequest(request);

        Headers headers = requestHeaders.headers();

        return new RequestRoute(
                HttpMethod.ofName(request.getMethod()),
                requestPath,
                QueryParameters.ofMap(request.getParameterMap()),
                headers,
                headers.getContentType(),
                Set.copyOf(headers.getAccept())
        );
    }
}
