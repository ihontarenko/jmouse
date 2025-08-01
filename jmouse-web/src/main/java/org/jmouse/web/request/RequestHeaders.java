package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.request.http.HttpHeader;

import java.util.Enumeration;

/**
 * 📦 Represents HTTP headers of a request as a structured {@link Headers} container.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * RequestHeaders requestHeaders = RequestHeaders.ofRequest(httpServletRequest);
 * }</pre>
 *
 * @param headers structured headers container
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RequestHeaders(Headers headers) {

    public static final String REQUEST_HEADERS_ATTRIBUTE = RequestHeaders.class.getName() + ".REQUEST_HEADERS";

    /**
     * 🔍 Builds {@code RequestHeaders} from a {@link HttpServletRequest}.
     *
     * <p>Only headers that match known {@link HttpHeader} values will be added.</p>
     *
     * @param request incoming servlet request
     * @return parsed {@code RequestHeaders} object
     */
    public static RequestHeaders ofRequest(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Headers             headers     = new Headers();

        while (headerNames.hasMoreElements()) {
            String     headerName = headerNames.nextElement();
            HttpHeader httpHeader = HttpHeader.ofHeader(headerName);

            if (httpHeader != null) {
                headers.setHeader(httpHeader, request.getHeader(httpHeader.value()));
            }
        }

        return new RequestHeaders(headers);
    }
}
