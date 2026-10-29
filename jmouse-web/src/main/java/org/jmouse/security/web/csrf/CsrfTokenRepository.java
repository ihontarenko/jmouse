package org.jmouse.security.web.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Owns token storage and must atomically load or issue the token for one client. */
public interface CsrfTokenRepository {
    CsrfToken loadOrCreate(HttpServletRequest request, HttpServletResponse response);
    void clear(HttpServletRequest request, HttpServletResponse response);
    boolean requiresSession();
}
