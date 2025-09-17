package org.jmouse.web.mvc.cors;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Allow;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.Vary;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;
import static org.jmouse.web.http.HttpHeader.*;

/**
 * 🌐 CORS processor for jMouse MVC.
 *
 * <p>Applies a {@link CorsConfiguration} to requests/responses:
 * sets {@code Vary}, validates {@code Origin}/{@code Access-Control-Request-*},
 * emits {@code Access-Control-Allow-*}, and short-circuits preflight with 204.</p>
 */
public class WebCorsProcessor implements CorsProcessor {

    /**
     * Wildcard value for “any origin”.
     */
    public static final String WILDCARD = "*";

    /**
     * Debug messages (set in {@code X-JMouse-Debug}).
     */
    public static final String INVALID_CORS_REQUEST     = "Invalid CORS request!";
    public static final String METHOD_IS_NOT_ALLOWED    = "CORS: requested method is not allowed";
    public static final String HEADERS_ARE_NOT_ALLOWED  = "CORS: requested headers are not allowed";
    public static final String ORIGIN_NOT_ALLOWED       = "CORS: Origin not allowed!";
    public static final String PREFLIGHT_REQUEST_PASSED = "CORS: Preflight request passed!";

    /**
     * Rejects the request with {@code 403 Forbidden} and writes a debug header.
     */
    private static boolean reject(Headers responseHeaders, String message) {
        responseHeaders.setStatus(HttpStatus.FORBIDDEN);
        responseHeaders.setHeader(HttpHeader.X_JMOUSE_DEBUG, requireNonNullElse(message, INVALID_CORS_REQUEST));
        return false;
    }

    /**
     * Entry point: sets {@code Vary}, skips non-CORS requests, and avoids double-processing
     * if {@code Access-Control-Allow-Origin} is already present; otherwise delegates.
     *
     * @param configuration   effective CORS policy
     * @param requestHeaders  incoming headers
     * @param responseHeaders buffered/target headers to mutate
     * @param preflight       whether this is a CORS preflight request
     * @return {@code true} to continue the handler chain; {@code false} if handled
     */
    @Override
    public boolean handleRequest(
            CorsConfiguration configuration, Headers requestHeaders, Headers responseHeaders, boolean preflight) {

        responseHeaders.setHeader(VARY, Vary.of(ORIGIN, ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS)
                .toHeaderValue());

        if (!Cors.isCorsRequest(requestHeaders)) {
            return true; // not a CORS request → nothing to do
        }

        if (responseHeaders.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            return true; // already processed upstream
        }

        return doHandleRequest(configuration, requestHeaders, responseHeaders, preflight);
    }

    /**
     * Core processing logic for actual and preflight CORS requests.
     *
     * <ol>
     *   <li>Validate origin against config.</li>
     *   <li>Resolve &amp; validate method.</li>
     *   <li>For preflight: validate requested headers; write allow-* headers, max-age, etc.</li>
     *   <li>For actual request: write allow-origin/credentials and expose-headers.</li>
     * </ol>
     *
     * @return {@code true} to continue the handler chain (actual request),
     *         or {@code false} if response is fully written (preflight or rejection).
     */
    protected boolean doHandleRequest(
            CorsConfiguration configuration, Headers requestHeaders, Headers responseHeaders, boolean preflight) {

        Matcher<String> originMatcher = new OriginMatcher(List.copyOf(configuration.getAllowedOrigins()));
        String          origin        = (String) requestHeaders.getHeader(ORIGIN);

        if (!checkOrigin(origin, originMatcher)) {
            return reject(responseHeaders, ORIGIN_NOT_ALLOWED);
        }

        HttpMethod httpMethod = getHttpMethod(requestHeaders, preflight);
        if (!checkHttpMethod(httpMethod, configuration)) {
            return reject(responseHeaders, METHOD_IS_NOT_ALLOWED);
        }

        List<HttpHeader> accessHeaders = getRequestHeaders(requestHeaders, preflight);
        if (preflight && !checkRequestHeaders(accessHeaders, configuration)) {
            return reject(responseHeaders, HEADERS_ARE_NOT_ALLOWED);
        }

        // Allow-Origin (+ credentials echo rule)
        Set<String> allowedOrigins   = configuration.getAllowedOrigins();
        boolean     allowCredentials = configuration.isAllowCredentials();
        boolean     isAnyOrigin      = allowedOrigins != null && allowedOrigins.contains(WILDCARD);
        String      allowOriginValue = (allowCredentials && isAnyOrigin) ? origin : (isAnyOrigin ? "*" : origin);

        responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, allowOriginValue);

        if (allowCredentials) {
            responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }

        if (preflight) {
            doHandlePreflight(configuration, requestHeaders, responseHeaders);
            return false;
        }

        // Exposed-Headers (actual request)
        Vary exposedVary = Vary.of(configuration.getExposedHeaders());
        if (!exposedVary.isEmpty()) {
            responseHeaders.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, exposedVary.toHeaderValue());
        }

        return true;
    }

    /**
     * Handles a CORS preflight request (OPTIONS with Origin + A-C-Request-*).
     *
     * <p>Writes the relevant {@code Access-Control-Allow-*} headers and sets {@code 204 No Content}.
     * Assumes the caller already validated origin, method, and requested headers.</p>
     *
     * <ul>
     *   <li>{@code Access-Control-Allow-Origin}: echoes the request origin, or {@code *} if wildcard
     *       and credentials are not allowed.</li>
     *   <li>{@code Access-Control-Allow-Credentials}: {@code true} when enabled.</li>
     *   <li>{@code Access-Control-Allow-Methods}: from config or falls back to the requested method.</li>
     *   <li>{@code Access-Control-Allow-Headers}: intersects requested with allowed, or echoes requested when allowed-set is empty.</li>
     *   <li>{@code Access-Control-Max-Age}: when configured &gt; 0.</li>
     *   <li>{@code Access-Control-Allow-Private-Network}: {@code true} when both requested and enabled (Chrome PNA draft).</li>
     * </ul>
     *
     * @param configuration   effective CORS policy
     * @param requestHeaders  incoming headers
     * @param responseHeaders outgoing/Buffered headers to mutate
     */
    protected void doHandlePreflight(
            CorsConfiguration configuration, Headers requestHeaders, Headers responseHeaders) {
        HttpMethod httpMethod = requestHeaders.getMethod();

        // Allow-Methods
        Allow allowMethods = getAllowedMethods(configuration.getAllowedMethods(), httpMethod);
        responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethods.toHeaderValue());

        // Allow-Headers
        Set<HttpHeader>  allowedHeaders   = configuration.getAllowedHeaders();
        List<HttpHeader> requestedHeaders = getRequestHeaders(requestHeaders, true);
        Vary             requestedVary    = Vary.of(requestedHeaders);

        if ((allowedHeaders == null || allowedHeaders.isEmpty()) && !requestedVary.isEmpty()) {
            responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, requestedVary.toHeaderValue());
        } else {
            Vary allowedVary = Vary.of(allowedHeaders);
            Vary grantedVary = allowedVary.intersect(requestedVary);
            if (!grantedVary.isEmpty()) {
                responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, grantedVary.toHeaderValue());
            }
        }

        // Max-Age
        if (configuration.getMaxAge() > 0) {
            responseHeaders.setHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(configuration.getMaxAge()));
        }

        // Private Network (Chrome private network access draft)
        if (configuration.isAllowPrivateNetwork()) {
            String requestPrivateNetwork = String.valueOf(requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK));
            if (Boolean.parseBoolean(requestPrivateNetwork)) {
                responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, "true");
            }
        }

        responseHeaders.setHeader(X_JMOUSE_DEBUG, PREFLIGHT_REQUEST_PASSED);
        responseHeaders.setStatus(HttpStatus.NO_CONTENT);
    }

    /**
     * Validates the {@code Origin} via the provided matcher.
     */
    public boolean checkOrigin(String origin, Matcher<String> originMatcher) {
        return originMatcher.matches(origin);
    }

    /**
     * Validates the HTTP method against configuration.
     *
     * <p>If {@code allowedMethods} is empty, this implementation treats it as “allow any”.</p>
     */
    public boolean checkHttpMethod(HttpMethod method, CorsConfiguration configuration) {
        Set<HttpMethod> allowed = configuration.getAllowedMethods();

        if (allowed.isEmpty()) {
            return true;
        }

        if (method == null) {
            return false;
        }

        return Allow.of(allowed).contains(method);
    }

    /**
     * Validates requested headers (preflight). Empty requested list is allowed.
     *
     * <p>If config's allowed headers are empty, treat as “allow requested”.</p>
     */
    public boolean checkRequestHeaders(List<HttpHeader> requestHeaders, CorsConfiguration configuration) {
        Set<HttpHeader> allowed = configuration.getAllowedHeaders();

        if (requestHeaders == null || requestHeaders.isEmpty() || allowed.isEmpty()) {
            return true;
        }

        for (HttpHeader httpHeader : requestHeaders) {
            if (!allowed.contains(httpHeader)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves the method: for preflight use {@code Access-Control-Request-Method},
     * otherwise use the actual request method.
     */
    private HttpMethod getHttpMethod(Headers requestHeaders, boolean preflight) {
        HttpMethod httpMethod = requestHeaders.getMethod();

        if (preflight) {
            httpMethod = HttpMethod.ofName((String) requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_METHOD));
        }

        return httpMethod;
    }

    /**
     * Resolves requested headers: for preflight parse {@code Access-Control-Request-Headers};
     * otherwise return the set of present header names (not typically used).
     */
    private List<HttpHeader> getRequestHeaders(Headers requestHeaders, boolean preflight) {
        List<HttpHeader> headers = List.copyOf(requestHeaders.asMap().keySet());

        if (preflight) {
            headers = Vary.of((String) requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_HEADERS)).asList();
        }

        return headers;
    }

    /**
     * Computes {@code Access-Control-Allow-Methods}:
     * if config is empty, falls back to the requested method.
     */
    private Allow getAllowedMethods(Set<HttpMethod> allowedMethods, HttpMethod fallback) {
        Allow allow = Allow.of(allowedMethods);

        if (allow.isEmpty()) {
            allow = allow.with(fallback);
        }

        return allow;
    }
}
