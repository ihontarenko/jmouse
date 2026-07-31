package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.http.HeaderWriter;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;
import org.jmouse.http.WWWAuthenticate;
import org.jmouse.web.http.writers.WWWAuthenticateWriter;

import java.io.IOException;

/**
 * 🛡️ AbstractRealmAuthenticationEntryPoint
 *
 * Base class for {@link AuthenticationEntryPoint} implementations
 * that send a {@code WWW-Authenticate} header with a configured realm.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🏷️ Maintain a configurable {@code realm} name</li>
 *   <li>📢 Add a {@code WWW-Authenticate} header via {@link #getWWWAuthenticate()}</li>
 *   <li>🚨 Always send {@code 401 Unauthorized}</li>
 * </ul>
 *
 * <p>Subclasses must implement {@link #getWWWAuthenticate()} to define
 * the authentication scheme (e.g. Basic, Bearer, Digest).</p>
 */
public abstract class AbstractRealmAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 🏷️ Realm name sent in the {@code WWW-Authenticate} header.
     */
    private String realm = "jMouse-Realm";

    /**
     * @return current realm name
     */
    public String getRealmName() {
        return realm;
    }

    /**
     * @param realm set a new realm name
     */
    public void setRealmName(String realm) {
        this.realm = realm;
    }

    /**
     * 🚨 Initiate authentication challenge:
     * <ul>
     *   <li>📢 Add {@code WWW-Authenticate} header from {@link #getWWWAuthenticate()}</li>
     *   <li>🚫 Send {@code 401 Unauthorized}</li>
     * </ul>
     *
     * @param request   HTTP request
     * @param response  HTTP response
     * @param exception optional triggering exception
     * @throws IOException if writing to response fails
     */
    @Override
    public void initiate(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        HeaderWriter headerWriter = new WWWAuthenticateWriter(getWWWAuthenticate());
        headerWriter.writeHeaders(request, response);
        response.sendError(HttpStatus.UNAUTHORIZED.getCode());
    }

    /**
     * 🔧 Provide the {@link WWWAuthenticate} instance (scheme + realm + params).
     *
     * @return configured challenge
     */
    protected abstract WWWAuthenticate getWWWAuthenticate();
}
