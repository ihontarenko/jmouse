package org.jmouse.security.web.session;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.request.RequestContextKeeper;

public class ChangeSessionIdAuthenticateHandler implements SessionAuthenticateHandler {

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        HttpServletRequest request     = keeper.request();
        var                session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
        } else {

        }
    }

}
