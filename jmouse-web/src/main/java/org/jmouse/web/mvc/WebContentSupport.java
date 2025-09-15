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
     * Returns a live, mutable view of the buffered HTTP headers.
     * <p>Changes to the returned instance affect what will be emitted
     * when headers are eventually written to the response.</p>
     *
     * @return modifiable headers collection (never {@code null})
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Clears all currently buffered headers.
     * <p>Use to discard previously staged values before composing a new response.
     * This does <em>not</em> remove headers that may have already been written to
     * the underlying {@code HttpServletResponse}.</p>
     */
    public void cleanupHeaders() {
        getHeaders().clear();
    }

    /**
     * Attempts to remove headers from the target {@link HttpServletResponse}
     * (when it is not yet committed) and/or clear buffered headers.
     * <p>This is a best-effort cleanup; behavior depends on the underlying
     * {@link HttpServletHeadersBuffer} implementation.</p>
     *
     * @param response target servlet response
     * @return {@code true} if any cleanup action was performed; {@code false} otherwise
     * @see HttpServletHeadersBuffer
     * @see jakarta.servlet.http.HttpServletResponse
     */
    public boolean cleanupHeaders(HttpServletResponse response) {
        cleanupHeaders();
        return new HttpServletHeadersBuffer(getHeaders()).cleanup(response);
    }

}
