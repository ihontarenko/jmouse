package org.jmouse.web.mvc.cors;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;

import java.util.HashSet;
import java.util.Set;

/**
 * 🔐 CORS utilities.
 *
 * <p>Provides helpers to detect CORS preflight requests.</p>
 */
public final class Cors {

    private Cors() {}

    /**
     * Determines whether the given headers represent a CORS preflight request.
     *
     * <p>Criteria (RFC 6454 / Fetch CORS):</p>
     * <ul>
     *   <li>HTTP method is {@code OPTIONS}</li>
     *   <li>{@code Origin} header is present</li>
     *   <li>{@code Access-Control-Request-Method} header is present</li>
     * </ul>
     *
     * @param headers request headers abstraction
     * @return {@code true} if the request is a CORS preflight; {@code false} otherwise
     * @see HttpMethod#OPTIONS
     * @see HttpHeader#ORIGIN
     * @see HttpHeader#ACCESS_CONTROL_REQUEST_METHOD
     */
    public static boolean isPreflight(final Headers headers) {
        return HttpMethod.OPTIONS == headers.getMethod()
                && headers.getHeader(HttpHeader.ORIGIN) != null
                && headers.getHeader(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }

    /**
     * Determines whether the current request (from {@link RequestAttributesHolder})
     * is a CORS preflight request.
     *
     * <p>Delegates to {@link #isPreflight(Headers)}.</p>
     *
     * @param request current servlet request (unused; kept for symmetry/convenience)
     * @return {@code true} if the request is a CORS preflight; {@code false} otherwise
     */
    public static boolean isPreflight(final HttpServletRequest request) {
        return isPreflight(RequestAttributesHolder.getRequestHeaders().headers());
    }

    /**
     * Determines whether the given request is a CORS request.
     *
     * <p>Criteria (RFC 6454):</p>
     * <ul>
     *   <li>{@code Origin} header is present</li>
     *   <li>{@code Origin} is not the same as the request's host (optional check)</li>
     * </ul>
     *
     * <p><em>Note:</em> For simplicity this implementation only checks
     * presence of {@code Origin}. Same-origin vs cross-origin detection
     * can be added if required.</p>
     *
     * @param headers request headers abstraction
     * @return {@code true} if it is a CORS request; {@code false} otherwise
     */
    public static boolean isCorsRequest(final Headers headers) {
        return headers.getHeader(HttpHeader.ORIGIN) != null;
    }

    /**
     * Variant for servlet request.
     *
     * @param request current servlet request
     * @return {@code true} if it is a CORS request; {@code false} otherwise
     */
    public static boolean isCorsRequest(final HttpServletRequest request) {
        return isCorsRequest(RequestAttributesHolder.getRequestHeaders().headers());
    }

    /**
     * 🔗 Combines two CORS configurations into a new one.
     *
     * <p>Merge rules:</p>
     * <ul>
     *   <li><b>allowedOrigins</b> = union(A, B)</li>
     *   <li><b>allowedMethods</b> = union(A, B)</li>
     *   <li><b>allowedHeaders</b> = union(A, B)</li>
     *   <li><b>exposedHeaders</b> = union(A, B)</li>
     *   <li><b>allowCredentials</b> = A || B</li>
     *   <li><b>maxAge</b> = max(A, B)</li>
     * </ul>
     *
     * <p>Returns a fresh {@link CorsConfiguration}; the inputs are not modified.</p>
     *
     * <p><em>Note:</em> If {@code "*"} is present in {@code allowedOrigins} and
     * {@code allowCredentials} is {@code true}, the CORS processor should ensure
     * a specific {@code Access-Control-Allow-Origin} is echoed (per the CORS spec).</p>
     *
     * @param configurationA first configuration (must not be {@code null})
     * @param configurationB second configuration (must not be {@code null})
     * @return merged configuration
     */
    public static CorsConfiguration combine(CorsConfiguration configurationA, CorsConfiguration configurationB) {
        CorsConfiguration combined = new CorsConfiguration();
        Set<String>       origins  = new HashSet<>(configurationA.getAllowedOrigins());
        Set<HttpMethod>   methods  = new HashSet<>(configurationA.getAllowedMethods());
        Set<HttpHeader>   allowed  = new HashSet<>(configurationA.getAllowedHeaders());
        Set<HttpHeader>   exposed  = new HashSet<>(configurationA.getExposedHeaders());

        origins.addAll(configurationB.getAllowedOrigins());
        combined.allowedOrigins(origins.toArray(new String[0]));

        methods.addAll(configurationB.getAllowedMethods());
        combined.allowedMethods(methods.toArray(new HttpMethod[0]));

        allowed.addAll(configurationB.getAllowedHeaders());
        combined.allowedHeaders(allowed.toArray(new HttpHeader[0]));

        exposed.addAll(configurationB.getExposedHeaders());
        combined.exposedHeaders(exposed.toArray(new HttpHeader[0]));

        combined.allowCredentials(configurationA.isAllowCredentials() || configurationB.isAllowCredentials());
        combined.maxAge(Math.max(configurationA.getMaxAge(), configurationB.getMaxAge()));

        return combined;
    }

    /**
     * Returns the {@code Origin} request header as a string.
     *
     * <p><em>Note:</em> This implementation uses {@link String#valueOf(Object)},
     * which yields the literal string {@code "null"} when the header is absent.</p>
     *
     * @param headers request headers abstraction
     * @return the {@code Origin} value (or the literal {@code "null"} if missing)
     * @see org.jmouse.web.http.HttpHeader#ORIGIN
     */
    public static String origin(Headers headers) {
        return String.valueOf(headers.getHeader(HttpHeader.ORIGIN));
    }

    /**
     * Returns the {@code Access-Control-Request-Method} preflight header as a string.
     *
     * <p><em>Note:</em> Uses {@link String#valueOf(Object)}, which returns the
     * literal {@code "null"} when the header is absent.</p>
     *
     * @param headers request headers abstraction
     * @return requested method value (or the literal {@code "null"} if missing)
     * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_REQUEST_METHOD
     */
    public static String requestedMethod(Headers headers) {
        return String.valueOf(headers.getHeader(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD));
    }

    /**
     * Returns the {@code Access-Control-Request-Headers} preflight header as a string.
     *
     * <p><em>Note:</em> Uses {@link String#valueOf(Object)}, which returns the
     * literal {@code "null"} when the header is absent.</p>
     *
     * @param headers request headers abstraction
     * @return requested headers value (comma-separated; or the literal {@code "null"} if missing)
     * @see org.jmouse.web.http.HttpHeader#ACCESS_CONTROL_REQUEST_HEADERS
     */
    public static String requestedHeaders(Headers headers) {
        return String.valueOf(headers.getHeader(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS));
    }


}
