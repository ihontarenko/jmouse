package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.Allow;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;
import java.util.Set;

import static org.jmouse.web.http.HttpHeader.ALLOW;
import static org.jmouse.web.http.HttpHeader.X_JMOUSE_DEBUG;
import static org.jmouse.web.http.HttpStatus.NO_CONTENT;

/**
 * HTTP {@code OPTIONS} handler that responds with {@code 204 No Content}
 * and an {@code Allow} header listing supported methods for the target resource.
 *
 * <p>Typical upstream logic computes the method set (e.g., add {@code OPTIONS},
 * remove {@code TRACE}, and include implicit {@code HEAD} when {@code GET} is present),
 * then delegates to this handler.</p>
 *
 * @see Allow
 * @see HttpMethod
 */
public class OptionsRequestHttpHandler implements RequestHttpHandler {

    /** Methods to advertise in the {@code Allow} header. */
    private final Set<HttpMethod> allowedMethods;

    /**
     * Creates an OPTIONS handler for the given set of methods.
     *
     * @param allowedMethods methods to expose in {@code Allow} (must not be {@code null})
     */
    public OptionsRequestHttpHandler(Set<HttpMethod> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    /**
     * Writes {@code 204 No Content}, sets {@code Allow}, and a debug header.
     *
     * <p>No response body is written. The {@code Allow} value is produced via
     * {@link Allow#toHeaderValue()}.</p>
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Allow  allow  = Allow.of(allowedMethods);
        String joined = allow.toHeaderValue();

        response.setStatus(NO_CONTENT.getCode());
        response.setHeader(ALLOW.value(), joined);
        response.setHeader(X_JMOUSE_DEBUG.value(), "OPTIONS Request: Allowed request-methods [" + joined + "]");
    }
}
