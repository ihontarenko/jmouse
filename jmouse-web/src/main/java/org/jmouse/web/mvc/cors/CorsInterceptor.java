package org.jmouse.web.mvc.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;
import org.jmouse.web.mvc.HandlerInterceptor;

/**
 * 🔐 MVC interceptor that applies a {@link CorsConfiguration} before handler execution.
 *
 * <p>Delegates to a {@link CorsProcessor} to evaluate the request (actual vs preflight),
 * mutate response headers accordingly, and decide whether to continue the chain.</p>
 *
 * <p>Typical behavior:</p>
 * <ul>
 *   <li><em>Preflight</em>: writes {@code Access-Control-Allow-*}, sets status (e.g., 204),
 *       returns {@code false} to short-circuit.</li>
 *   <li><em>Actual request</em>: writes {@code Access-Control-Allow-Origin} (+ credentials/expose),
 *       returns {@code true} to proceed.</li>
 * </ul>
 *
 * @see CorsConfiguration
 * @see CorsProcessor
 * @see org.jmouse.web.http.HttpHeader
 */
public class CorsInterceptor implements HandlerInterceptor {

    private final boolean           preflight;
    private final CorsConfiguration configuration;
    private final CorsProcessor     processor;

    /**
     * Creates a CORS interceptor.
     *
     * @param preflight      whether this interceptor handles CORS preflight exchanges
     * @param configuration  effective CORS policy to apply
     * @param processor      strategy that evaluates request/headers and writes response headers
     */
    public CorsInterceptor(boolean preflight, CorsConfiguration configuration, CorsProcessor processor) {
        this.preflight = preflight;
        this.configuration = configuration;
        this.processor = processor;
    }

    /**
     * Applies CORS policy and writes headers before the handler is invoked.
     *
     * <p>Builds a temporary {@link Headers} buffer for the response, delegates to the
     * {@link CorsProcessor}, then flushes headers into the {@link HttpServletResponse}.
     * The boolean return value controls whether the execution chain continues.</p>
     *
     * @return {@code false} to short-circuit (typical for preflight), {@code true} to continue
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Headers responseHeaders = new Headers();
        Headers requestHeaders  = RequestAttributesHolder.getRequestRoute().headers();
        boolean result          = processor.handleRequest(configuration, requestHeaders, responseHeaders, preflight);

        new HttpServletHeadersBuffer(responseHeaders).write(response);

        return result;
    }
}
