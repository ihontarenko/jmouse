package org.jmouse.web.mvc.cors;

import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🌐 Declarative CORS mapping for controllers and handler methods.
 *
 * <p>Values mirror {@link CorsConfiguration} semantics and are consumed by the CORS processor.
 * Empty arrays generally mean “no restriction” (processor-specific: allow any/requested).</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @CorsMapping(
 *     origins = {"https://example.com", "https://*.example.org:[80,443]"},
 *     methods = {HttpMethod.GET, HttpMethod.POST},
 *     allowedHeaders = {HttpHeader.CONTENT_TYPE, HttpHeader.AUTHORIZATION},
 *     exposedHeaders = {HttpHeader.CONTENT_LENGTH},
 *     allowCredentials = true,
 *     maxAge = 3600
 * )
 * }</pre>
 *
 * @see CorsConfiguration
 * @see WebCorsProcessor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsMapping {

    /**
     * Allowed origins (exact values, wildcards like {@code https://*.example.com},
     * and optional port lists {@code :[80,443]} supported by the processor).
     * <p>Use {@code "*"} to allow any origin (not with credentials).</p>
     */
    String[] origins() default {};

    /**
     * Allowed HTTP methods for CORS checks.
     * <p>Empty = no explicit restriction (processor may allow requested method).</p>
     */
    HttpMethod[] methods() default {};

    /**
     * Request headers allowed on preflight ({@code Access-Control-Allow-Headers}).
     * <p>Empty = allow requested headers.</p>
     */
    HttpHeader[] allowedHeaders() default {};

    /**
     * Response headers exposed to the client on actual requests
     * ({@code Access-Control-Expose-Headers}).
     * <p>Empty = expose none.</p>
     */
    HttpHeader[] exposedHeaders() default {};

    /**
     * Whether to allow credentials ({@code Access-Control-Allow-Credentials:true}).
     * <p>When {@code true}, the processor should echo a specific origin, not {@code "*"}.</p>
     */
    boolean allowCredentials() default false;

    /**
     * Preflight cache duration in seconds ({@code Access-Control-Max-Age}).
     * <p>Default: 1800 (30 minutes).</p>
     */
    long maxAge() default 1800;
}
