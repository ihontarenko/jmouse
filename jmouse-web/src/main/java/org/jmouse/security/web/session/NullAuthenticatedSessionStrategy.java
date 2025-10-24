package org.jmouse.security.web.session;

import org.jmouse.security.Authentication;
import org.jmouse.web.http.RequestContextKeeper;

public class NullAuthenticatedSessionStrategy implements SessionAuthenticateHandler {
    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {

    }
}
