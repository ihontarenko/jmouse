package org.jmouse.security.web.authentication.www;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.WWWAuthenticate;

import java.io.IOException;

/**
 * 🔐 BasicAuthenticationEntryPoint
 * <p>
 * Initiates an HTTP Basic authentication challenge when an unauthenticated
 * client tries to access a protected resource.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📢 Adds a {@code WWW-Authenticate} header with the configured realm</li>
 *   <li>⚠️ Sets response status to {@code 401 Unauthorized}</li>
 *   <li>📝 Optionally writes a plain-text body ("UNAUTHORIZED!")</li>
 * </ul>
 */
public class BasicAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 🏷️ Realm name sent in the {@code WWW-Authenticate} header.
     */
    private String realm = "jMouse-Realm";

    /**
     * 📝 Whether to write a body message in addition to the 401 header.
     */
    private boolean writeBody = false;

    /**
     * 🚨 Initiate a Basic Authentication challenge.
     *
     * @param request   current HTTP request
     * @param response  current HTTP response
     * @param exception optional auth exception that triggered entry point
     * @throws IOException if writing to response fails
     */
    @Override
    public void initiate(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        HttpHeader      httpHeader      = HttpHeader.WWW_AUTHENTICATE;
        WWWAuthenticate wwwAuthenticate = WWWAuthenticate.basic().realm(getRealmName());
        HttpStatus      httpStatus      = HttpStatus.UNAUTHORIZED;

        // Add challenge header
        response.setHeader(httpHeader.value(), wwwAuthenticate.toHeaderValue());

        // Optionally write body
        if (isWriteBody()) {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("UNAUTHORIZED!");
        }

        // Always send 401
        response.sendError(httpStatus.getCode());
    }

    /**
     * @return configured realm name
     */
    public String getRealmName() {
        return realm;
    }

    /**
     * @param realm set the realm name for the Basic challenge
     */
    public void setRealmName(String realm) {
        this.realm = realm;
    }

    /**
     * @return true if plain text body should be written
     */
    public boolean isWriteBody() {
        return writeBody;
    }

    /**
     * @param writeBody enable or disable writing plain text body
     */
    public void setWriteBody(boolean writeBody) {
        this.writeBody = writeBody;
    }
}
