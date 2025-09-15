package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Allow;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.Vary;

import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 🛠️ Generalized responder for HTTP responses, inspired by Spring's {@code WebContentGenerator}.
 *
 * <p>Provides common features:</p>
 * <ul>
 *   <li>Restricting supported HTTP methods (with automatic {@code Allow} header management).</li>
 *   <li>Optional session requirement check.</li>
 *   <li>Cache control policies ({@code Cache-Control}, {@code Pragma}, {@code Expires}).</li>
 *   <li>Support for {@code Vary} header.</li>
 *   <li>Automatic handling of {@code OPTIONS} requests.</li>
 * </ul>
 *
 * @see CacheControl
 * @see Vary
 * @see Allow
 * @see HttpMethod
 */
public abstract class WebResponder extends WebContentSupport {

    private Set<HttpMethod> supported;      // null → unrestricted (except TRACE)
    private boolean         requireSession; // hook; enforce in your code if needed
    private CacheControl    cacheControl;   // preferred
    private int             cacheSeconds = -1; // fallback
    private Vary            vary;           // optional
    private Allow           allow;          // computed

    /**
     * Creates a responder allowing GET, HEAD, and POST by default.
     */
    protected WebResponder() {
        this(true);
    }

    /**
     * Creates a responder with configurable default restriction.
     *
     * @param restrictDefault if {@code true}, defaults to GET/HEAD/POST; if {@code false}, unrestricted
     */
    protected WebResponder(boolean restrictDefault) {
        if (restrictDefault) {
            setSupportedMethods(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST);
        }
        configureAllowValues();
    }

    /**
     * Creates a responder with explicit supported methods.
     *
     * @param methods allowed HTTP methods
     */
    protected WebResponder(HttpMethod... methods) {
        setSupportedMethods(methods);
    }

    /**
     * @return supported methods, or {@code null} if unrestricted (except TRACE)
     */
    public final HttpMethod[] getSupportedMethods() {
        return (this.supported != null ? this.supported.toArray(HttpMethod[]::new) : null);
    }

    /**
     * Sets supported methods. Passing {@code null} or empty array means unrestricted (except TRACE).
     *
     * @param methods allowed HTTP methods
     */
    public final void setSupportedMethods(HttpMethod... methods) {
        boolean isAllowMethodPresent = methods != null && methods.length > 0;
        this.supported = isAllowMethodPresent ? new LinkedHashSet<>(List.of(methods)) : null;
        configureAllowValues();
    }

    /**
     * @return {@code true} if a pre-existing session is required
     */
    public final boolean isRequireSession() {
        return requireSession;
    }

    /**
     * Configures whether a pre-existing session is required.
     */
    public final void setRequireSession(boolean require) {
        this.requireSession = require;
    }

    /**
     * @return configured {@link CacheControl}, may be {@code null}
     */
    public final CacheControl getCacheControl() {
        return cacheControl;
    }

    /**
     * Sets the cache control policy (preferred over {@link #setCacheSeconds(int)}).
     */
    public final void setCacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
    }

    /**
     * @return configured cache lifetime in seconds, or {@code -1} if unset
     */
    public final int getCacheSeconds() {
        return cacheSeconds;
    }

    /**
     * Sets cache lifetime in seconds.
     * <ul>
     *   <li>{@code >0} → {@code Cache-Control: max-age}</li>
     *   <li>{@code 0} → {@code Cache-Control: no-store}</li>
     *   <li>{@code <0} → no explicit cache header</li>
     * </ul>
     */
    public final void setCacheSeconds(int seconds) {
        this.cacheSeconds = seconds;
    }

    /**
     * @return configured {@link Vary} header, or {@code null}
     */
    public final Vary getVary() {
        return vary;
    }

    /**
     * Sets {@link Vary} header values.
     */
    public final void setVary(Vary vary) {
        this.vary = vary;
    }

    /**
     * @return computed {@link Allow} header value
     */
    protected final Allow getAllow() {
        return allow;
    }

    /**
     * Handles {@code OPTIONS} requests by writing {@code Allow} header and returning 204.
     *
     * @param method   incoming HTTP method
     * @param response servlet response
     * @return {@code true} if OPTIONS was handled, {@code false} otherwise
     */
    protected boolean maybeHandleOptions(HttpMethod method, HttpServletResponse response) {
        if (method == HttpMethod.OPTIONS) {
            response.setHeader(HttpHeader.ALLOW.value(), allow.toHeaderValue());
            response.setStatus(HttpStatus.NO_CONTENT.getCode());
            return true;
        }
        return false;
    }

    /**
     * Validates the request against supported methods and session requirements.
     *
     * @param method        request method
     * @param sessionExists whether a session exists
     * @throws IllegalStateException if the method is unsupported or session is required but absent
     */
    protected final void checkRequest(HttpMethod method, boolean sessionExists) throws IllegalStateException {
        if (!getAllow().isEmpty() && !getAllow().contains(method)) {
            throw new IllegalStateException(
                    "HTTP method not supported: %s (Allow: %s)".formatted(method, allow.toHeaderValue()));
        }
        if (isRequireSession() && !sessionExists) {
            throw new IllegalStateException("Pre-existing session required but none found");
        }
    }

    /**
     * Prepares the response by writing {@code Cache-Control} and {@code Vary} headers into the buffer.
     * <p>Call this before {@link #writeHeaders(HttpServletResponse)}.</p>
     */
    protected final void prepareResponse() {
        Headers      headers      = getHeaders();
        CacheControl cacheControl = getCacheControl();

        if (cacheControl == null) {
            if (cacheSeconds > 0) {
                cacheControl = CacheControl.empty().maxAge(Duration.ofSeconds(cacheSeconds));
            } else if (cacheSeconds == 0) {
                cacheControl = CacheControl.empty().noStore();
            } else {
                cacheControl = CacheControl.empty();
            }
        }

        if (cacheControl != null) {
            String cacheControlValue = cacheControl.toHeaderValue();
            if (cacheControlValue != null && !cacheControlValue.isBlank()) {
                headers.setHeader(HttpHeader.CACHE_CONTROL, cacheControlValue);
                headers.removeHeader(HttpHeader.PRAGMA);
                headers.removeHeader(HttpHeader.EXPIRES);
            }
        }

        if (getVary() != null && !getVary().isEmpty()) {
            getVary().writeTo(headers);
        }
    }

    /**
     * Computes the {@code Allow} header value based on supported methods.
     */
    private void configureAllowValues() {
        if (supported == null) {
            EnumSet<HttpMethod> withoutTrace = EnumSet.allOf(HttpMethod.class);
            withoutTrace.remove(HttpMethod.TRACE);
            withoutTrace.add(HttpMethod.OPTIONS);
            this.allow = Allow.of(List.copyOf(withoutTrace));
        } else {
            Set<HttpMethod> unique = new LinkedHashSet<>(supported);
            unique.add(HttpMethod.OPTIONS);
            this.allow = Allow.of(List.copyOf(unique));
        }
    }
}
