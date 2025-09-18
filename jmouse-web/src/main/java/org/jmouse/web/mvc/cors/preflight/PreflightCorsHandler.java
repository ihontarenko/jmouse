package org.jmouse.web.mvc.cors.preflight;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;

import static org.jmouse.web.http.HttpHeader.X_JMOUSE_DEBUG;

/**
 * No-op handler used for CORS preflight exchanges.
 *
 * <p>Intended as a fallback when the CORS interceptor has already written all
 * required {@code Access-Control-Allow-*} headers and short-circuited the chain.
 * This handler does not write a body or status; it only adds a debug header.</p>
 *
 * <p>Typically paired with {@code CorsInterceptor(preflight = true)}.</p>
 */
public class PreflightCorsHandler implements RequestHttpHandler {

    /**
     * Adds a debug header indicating a preflight CORS request.
     *
     * <p>Assumes headers/status have been set by the CORS processor.</p>
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader(X_JMOUSE_DEBUG.value(), "PREFLIGHT CORS REQUEST SUCCESSFULLY!");
    }

    /**
     * @return human-readable identifier for diagnostics
     */
    @Override
    public String toString() {
        return "PREFLIGHT_CORS_HANDLER: NO-OP";
    }
}
