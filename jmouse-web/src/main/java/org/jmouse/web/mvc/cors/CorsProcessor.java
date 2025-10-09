package org.jmouse.web.mvc.cors;

import org.jmouse.web.http.Headers;

/**
 * 🔐 Strategy for applying a CORS policy to an HTTP exchange.
 *
 * <p>Implementations inspect request headers and write appropriate
 * {@code Access-Control-*} response headers according to the provided
 * {@link CorsConfiguration}. May short-circuit preflight requests.</p>
 *
 * @see CorsConfiguration
 * @see org.jmouse.web.http.HttpHeader#ORIGIN
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_REQUEST_METHOD
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_ALLOW_ORIGIN
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_ALLOW_METHODS
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_ALLOW_HEADERS
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_EXPOSE_HEADERS
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_ALLOW_CREDENTIALS
 * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_MAX_AGE
 */
public interface CorsProcessor {

    /**
     * Applies the given CORS configuration to the current request/response.
     *
     * <p><b>Typical behavior</b>:</p>
     * <ul>
     *   <li><em>Preflight</em> ({@code OPTIONS} with {@code Origin} and
     *       {@code Access-Control-Request-Method}): emit
     *       {@code Access-Control-Allow-Origin}, {@code -Methods},
     *       {@code -Headers}, {@code -Credentials}, {@code -Max-Age}.</li>
     *   <li><em>Actual request</em>: emit {@code Access-Control-Allow-Origin},
     *       optional {@code -Credentials}, and {@code Access-Control-Expose-Headers}.</li>
     * </ul>
     *
     * <p><em>Note:</em> When credentials are allowed, do not use {@code "*"} as
     * {@code Access-Control-Allow-Origin}; echo a specific origin.</p>
     *
     * @param configuration   effective CORS policy (non-null)
     * @param requestHeaders  incoming request headers
     * @param responseHeaders outgoing/Buffered response headers to mutate
     * @param preflight       {@code true} if this is a CORS preflight request
     */
    boolean handleRequest(
            CorsConfiguration configuration,
            Headers requestHeaders,
            Headers responseHeaders,
            boolean preflight);
}
