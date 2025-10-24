package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.Authentication;

abstract public class AbstractAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication provide(HttpServletRequest request) {
        return doProvide(request);
    }

    abstract protected Authentication doProvide(HttpServletRequest request);

}
