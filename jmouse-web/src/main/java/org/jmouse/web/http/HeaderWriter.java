package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 📝 {@code HeaderWriter}
 *
 * Contract for writing HTTP headers into a {@link HttpServletResponse}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📥 Inspect the current {@link HttpServletRequest}/{@link HttpServletResponse}</li>
 *   <li>📤 Write or override security/utility headers</li>
 *   <li>🛑 Avoid writing if the response is already committed</li>
 * </ul>
 *
 * <p>💡 Typically used in web security for setting headers like
 * {@code Cache-Control}, {@code Strict-Transport-Security}, or {@code X-Frame-Options}.</p>
 */
public interface HeaderWriter {

    /**
     * ✍️ Write headers to the given request/response pair.
     *
     * @param request  current request
     * @param response current response
     */
    void writeHeaders(HttpServletRequest request, HttpServletResponse response);

    /**
     * ✍️ Convenience variant using {@link RequestContext}.
     *
     * @param requestContext wrapper holding request/response
     */
    default void writeHeaders(RequestContext requestContext) {
        writeHeaders(requestContext.request(), requestContext.response());
    }

    /**
     * ➕ Write a single header if not already present and response not committed.
     *
     * @param response    target response
     * @param httpHeader  header type
     * @param headerValue value to add
     */
    default void writeHeader(HttpServletResponse response, HttpHeader httpHeader, Object headerValue) {
        if (!response.isCommitted() && !response.containsHeader(httpHeader.value())) {
            response.addHeader(httpHeader.value(), headerValue.toString());
        }
    }
}
