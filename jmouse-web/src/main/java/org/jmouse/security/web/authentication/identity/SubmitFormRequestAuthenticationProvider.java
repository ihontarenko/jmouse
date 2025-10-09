package org.jmouse.security.web.authentication.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;

/**
 * 📝 SubmitFormRequestAuthenticationProvider
 * <p>
 * Extracts username and password credentials from an HTTP form submission.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📥 Reads request parameters (username + password)</li>
 *   <li>🔑 Builds a {@link UsernamePasswordAuthentication} token</li>
 *   <li>🧩 Pluggable into an authentication filter chain</li>
 * </ul>
 *
 * <p>💡 Typically used with {@code application/x-www-form-urlencoded} POST requests
 * from a login form.</p>
 */
public class SubmitFormRequestAuthenticationProvider extends AbstractAuthenticationProvider {

    /**
     * 🔑 Name of the request parameter containing the username.
     */
    private final String usernameParameter;

    /**
     * 🔒 Name of the request parameter containing the password.
     */
    private final String passwordParameter;

    /**
     * 🏗️ Create a provider with custom parameter names.
     *
     * @param usernameParameter form field name for username
     * @param passwordParameter form field name for password
     */
    public SubmitFormRequestAuthenticationProvider(String usernameParameter, String passwordParameter) {
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
    }

    /**
     * @return username parameter name
     */
    public String getUsernameParameter() {
        return usernameParameter;
    }

    /**
     * @return password parameter name
     */
    public String getPasswordParameter() {
        return passwordParameter;
    }

    /**
     * 📥 Extract credentials from request and wrap in {@link UsernamePasswordAuthentication}.
     *
     * @param request incoming HTTP request
     * @return authentication token (never null, values may be empty strings if missing)
     */
    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        String username = request.getParameter(getUsernameParameter());
        String password = request.getParameter(getPasswordParameter());
        return new UsernamePasswordAuthentication(username, password);
    }
}
