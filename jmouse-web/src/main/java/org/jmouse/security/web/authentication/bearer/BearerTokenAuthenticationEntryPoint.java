package org.jmouse.security.web.authentication.bearer;

import org.jmouse.security.web.authentication.AbstractRealmAuthenticationEntryPoint;
import org.jmouse.web.http.request.WWWAuthenticate;
import org.jmouse.web.http.request.WWWAuthenticate.Bearer;

/**
 * 🪙 BearerTokenAuthenticationEntryPoint
 *
 * Sends an OAuth2 Bearer token authentication challenge with optional
 * error details according to <a href="https://datatracker.ietf.org/doc/html/rfc6750#section-3">
 * RFC 6750 (Bearer Token Usage)</a>.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📢 Builds a {@code WWW-Authenticate: Bearer ...} header</li>
 *   <li>🚫 Ensures response is {@code 401 Unauthorized}</li>
 *   <li>🏷️ Includes optional {@code realm}, {@code error}, and {@code error_description}</li>
 * </ul>
 *
 * <p>Typical error codes:</p>
 * <ul>
 *   <li><b>invalid_token</b> – expired, malformed, or signature invalid</li>
 *   <li><b>insufficient_scope</b> – token lacks required permissions</li>
 *   <li><b>invalid_request</b> – malformed or missing authentication information</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
 * entryPoint.setRealmName("App-API");
 * entryPoint.setErrorCode("invalid_token");
 * entryPoint.setErrorDescription("The access token is expired");
 * }</pre>
 */
public class BearerTokenAuthenticationEntryPoint extends AbstractRealmAuthenticationEntryPoint {

    /**
     * 📝 Human-readable description of the error.
     */
    private String errorDescription;

    /**
     * ❌ Machine-readable error code (e.g., "invalid_token").
     */
    private String errorCode;

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 🔧 Provides the Bearer token challenge with realm and optional error details.
     *
     * @return configured {@link WWWAuthenticate} instance
     */
    @Override
    protected WWWAuthenticate getWWWAuthenticate() {
        Bearer           bearer = WWWAuthenticate.bearer(getRealmName());
        Bearer.ErrorCode code   = Bearer.ErrorCode.valueOf(getErrorCode().toUpperCase());

        bearer.error(code);
        bearer.errorDescription(getErrorDescription());

        return bearer;
    }
}
