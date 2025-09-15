package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.response.HeadersBuffer;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;

/**
 * 🔧 Base support for web content responses.
 *
 * <p>Provides a {@link Headers} abstraction for managing HTTP headers,
 * allowing delayed application and optional cleanup before the response is committed.</p>
 *
 * <p>Intended to be extended by controllers or handlers that need
 * to manage HTTP response headers consistently.</p>
 *
 * @see HeadersBuffer
 * @see HttpServletResponse
 */
public abstract class WebContentSupport {

    /**
     * Internal headers for response.
     */
    private final Headers headers = new Headers();

    /**
     * Writes all buffered headers into the given {@link HttpServletResponse}.
     *
     * @param response target servlet response
     */
    public void writeHeaders(HttpServletResponse response) {
        new HttpServletHeadersBuffer(getHeaders()).write(response);
    }


    /**
     * Returns the {@link Headers} view from the internal buffer.
     *
     * @return modifiable headers collection
     */
    public Headers getHeaders() {
        return headers;
    }
}
