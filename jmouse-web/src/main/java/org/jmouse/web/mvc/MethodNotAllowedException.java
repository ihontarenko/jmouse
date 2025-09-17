package org.jmouse.web.mvc;

import org.jmouse.web.http.HttpMethod;

import java.util.Set;

/**
 * ❌ Exception indicating HTTP {@code 405 Method Not Allowed}.
 *
 * <p>Carries the set of allowed {@link HttpMethod}s for the requested resource.
 * Callers can use this to render the {@code Allow} response header.</p>
 *
 * @see HttpMethod
 */
public class MethodNotAllowedException extends RuntimeException {

    private final Set<HttpMethod> allowedMethods;

    /**
     * Creates a new {@code MethodNotAllowedException}.
     *
     * @param allowedMethods methods permitted for the target resource (used for {@code Allow} header)
     * @param message        human-readable detail message
     */
    public MethodNotAllowedException(Set<HttpMethod> allowedMethods, String message) {
        super(message);
        this.allowedMethods = allowedMethods;
    }

    /**
     * Returns the methods permitted for the target resource.
     *
     * @return set of allowed {@link HttpMethod}s (never {@code null})
     */
    public Set<HttpMethod> getAllowedMethods() {
        return allowedMethods;
    }
}
