package org.jmouse.web.mvc.cors;

import org.jmouse.core.Streamable;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jmouse.web.http.HttpMethod.*;


/**
 * 🌐 CORS configuration for jMouse MVC.
 *
 * <p>Collects policy fields:</p>
 * <ul>
 *   <li>allowedOrigins</li>
 *   <li>allowedMethods</li>
 *   <li>allowedHeaders</li>
 *   <li>exposedHeaders</li>
 *   <li>allowCredentials</li>
 *   <li>maxAge</li>
 * </ul>
 *
 * <p>Intended to be consumed by a CORS processor that emits
 * {@code Access-Control-Allow-*} and {@code Access-Control-Expose-Headers}.</p>
 *
 * @see HttpHeader#ORIGIN
 * @see HttpHeader#ACCESS_CONTROL_REQUEST_METHOD
 * @see HttpHeader#ACCESS_CONTROL_ALLOW_ORIGIN
 * @see HttpHeader#ACCESS_CONTROL_ALLOW_METHODS
 * @see HttpHeader#ACCESS_CONTROL_ALLOW_HEADERS
 * @see HttpHeader#ACCESS_CONTROL_EXPOSE_HEADERS
 * @see HttpHeader#ACCESS_CONTROL_ALLOW_CREDENTIALS
 * @see HttpHeader#ACCESS_CONTROL_MAX_AGE
 */
public class CorsConfiguration {

    public static final List<HttpMethod> DEFAULT_ALLOWED_METHODS = List.of(GET, HEAD, POST);

    /**
     * Accepted values for {@code Origin}. Use {@code "*"} for all (not with credentials).
     */
    private final Set<String> allowedOrigins = new HashSet<>(8);

    /**
     * Methods to list in {@code Access-Control-Allow-Methods}.
     */
    private final Set<HttpMethod> allowedMethods = new HashSet<>(8);

    /**
     * Request headers the server allows in preflight ({@code Access-Control-Allow-Headers}).
     */
    private final Set<HttpHeader> allowedHeaders = new HashSet<>(8);

    /**
     * Response headers exposed to the client ({@code Access-Control-Expose-Headers}).
     */
    private final Set<HttpHeader> exposedHeaders = new HashSet<>(8);

    /**
     * Whether to emit {@code Access-Control-Allow-Credentials:true}.
     */
    private boolean allowCredentials = false;

    private boolean allowPrivateNetwork = false;

    /**
     * Preflight cache duration in seconds ({@code Access-Control-Max-Age}). Default: 1800 (30 min).
     */
    private long maxAge = 1800; // default 30 min

    /**
     * Adds allowed origins (exact matches or {@code "*"}).
     *
     * @param origins origin strings, e.g. {@code "https://example.com"}
     * @return this config (for chaining)
     */
    public CorsConfiguration allowedOrigins(String... origins) {
        allowedOrigins.addAll(List.of(origins));
        return this;
    }

    /**
     * Adds allowed methods by name.
     *
     * <p>Names are resolved via {@link HttpMethod#ofName(String)}.</p>
     *
     * @param methods method names (e.g., {@code "GET"}, {@code "POST"})
     * @return this config (for chaining)
     */
    public CorsConfiguration allowedMethods(String... methods) {
        return allowedMethods(Streamable.of(methods).map(HttpMethod::ofName).toArray(HttpMethod[]::new));
    }

    /**
     * Adds allowed methods.
     *
     * @param methods enum constants to allow
     * @return this config (for chaining)
     */
    public CorsConfiguration allowedMethods(HttpMethod... methods) {
        allowedMethods.addAll(List.of(methods));
        return this;
    }

    /**
     * Adds allowed request headers by name.
     *
     * <p>Names are resolved via {@link HttpHeader#ofHeader(String)}.</p>
     *
     * @param headers header names
     * @return this config (for chaining)
     */
    public CorsConfiguration allowedHeaders(String... headers) {
        return allowedHeaders(Streamable.of(headers).map(HttpHeader::ofHeader).toArray(HttpHeader[]::new));
    }

    /**
     * Adds allowed request headers.
     *
     * @param headers header enums
     * @return this config (for chaining)
     */
    public CorsConfiguration allowedHeaders(HttpHeader... headers) {
        allowedHeaders.addAll(List.of(headers));
        return this;
    }

    /**
     * Adds exposed response headers by name.
     *
     * <p>Names are resolved via {@link HttpHeader#ofHeader(String)}.</p>
     *
     * @param headers header names
     * @return this config (for chaining)
     */
    public CorsConfiguration exposedHeaders(String... headers) {
        return exposedHeaders(Streamable.of(headers).map(HttpHeader::ofHeader).toArray(HttpHeader[]::new));
    }

    /**
     * Adds exposed response headers.
     *
     * @param headers header enums
     * @return this config (for chaining)
     */
    public CorsConfiguration exposedHeaders(HttpHeader... headers) {
        exposedHeaders.addAll(List.of(headers));
        return this;
    }

    /**
     * Enables or disables credential support.
     *
     * <p><strong>Note:</strong> When credentials are allowed, {@code Access-Control-Allow-Origin}
     * must not be {@code "*"}; the processor should echo a specific origin.</p>
     *
     * @param allow whether to allow credentials
     * @return this config (for chaining)
     */
    public CorsConfiguration allowCredentials(boolean allow) {
        this.allowCredentials = allow;
        return this;
    }

    public CorsConfiguration allowPrivateNetwork(boolean privateNetwork) {
        this.allowPrivateNetwork = privateNetwork;
        return this;
    }

    /**
     * Sets the preflight cache duration.
     *
     * @param seconds seconds for {@code Access-Control-Max-Age}
     * @return this config (for chaining)
     */
    public CorsConfiguration maxAge(long seconds) {
        this.maxAge = seconds;
        return this;
    }

    /**
     * @return configured allowed origins
     */
    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * @return configured allowed methods
     */
    public Set<HttpMethod> getAllowedMethods() {
        return allowedMethods;
    }

    /**
     * @return configured allowed request headers
     */
    public Set<HttpHeader> getAllowedHeaders() {
        return allowedHeaders;
    }

    /**
     * @return configured exposed response headers
     */
    public Set<HttpHeader> getExposedHeaders() {
        return exposedHeaders;
    }

    /**
     * @return whether credentials are allowed
     */
    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public boolean isAllowPrivateNetwork() {
        return allowPrivateNetwork;
    }

    /**
     * @return preflight cache duration in seconds
     */
    public long getMaxAge() {
        return maxAge;
    }
}
