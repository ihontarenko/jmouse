package org.jmouse.security.web.session;

import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.request.RequestContextKeeper;

public class NullAuthenticatedSessionStrategy implements SessionAuthenticationStrategy {
    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {

    }
}
