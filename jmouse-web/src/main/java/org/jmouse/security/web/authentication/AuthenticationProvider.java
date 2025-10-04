package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.core.Authentication;

public interface AuthenticationProvider {
    Authentication provide(HttpServletRequest request);
}
