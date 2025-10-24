package org.jmouse.security.web.authentication.bearer;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.BearerTokenAuthentication;
import org.jmouse.security.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.Headers;
import org.jmouse.web.http.RequestHeaders;

public class BearerTokenAuthenticationProvider extends AbstractAuthenticationProvider {

    public static final String BEARER_PREFIX = "Bearer";

    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        Headers headers = RequestHeaders.ofRequest(request).headers();
        String  header  = (String) headers.getHeader(HttpHeader.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return new BearerTokenAuthentication(
                    BEARER_PREFIX.toLowerCase(), header.substring(BEARER_PREFIX.length()).trim());
        }

        return null;
    }

}
