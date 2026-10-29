package org.jmouse.security.web.authentication.bearer;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.Authentication;
import org.jmouse.security.BadCredentialsException;
import org.jmouse.security.authentication.BearerTokenAuthentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;
import org.jmouse.security.web.authentication.AuthorizationHeaderCredentials;

public class BearerTokenAuthenticationProvider extends AbstractAuthenticationProvider {
    public static final String BEARER_PREFIX = "Bearer";

    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        String value = AuthorizationHeaderCredentials.read(request, BEARER_PREFIX);
        if (value == null) {
            return null;
        }
        if (!value.matches("[A-Za-z0-9._~+/-]+=*")) {
            throw new BadCredentialsException("Malformed bearer token");
        }
        return new BearerTokenAuthentication("bearer", value);
    }
}
