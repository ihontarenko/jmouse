package org.jmouse.security.web.session;

import org.jmouse.security.Authentication;
import org.jmouse.web.http.RequestContextKeeper;

public interface SessionAuthenticateHandler {
    String APPLIED_ATTRIBUTE = SessionAuthenticateHandler.class.getName() + ".APPLIED";

    void onAuthentication(Authentication authentication, RequestContextKeeper keeper);
}