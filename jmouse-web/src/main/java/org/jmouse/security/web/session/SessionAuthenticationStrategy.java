package org.jmouse.security.web.session;

import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.request.RequestContextKeeper;

public interface SessionAuthenticationStrategy {
    void onAuthentication(Authentication authentication, RequestContextKeeper keeper);
}