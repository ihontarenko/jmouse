package org.jmouse.security.web.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jmouse.security.Authentication;
import org.jmouse.web.http.RequestContextKeeper;

public class ChangeSessionIdAuthenticateHandler implements SessionAuthenticateHandler {

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        HttpServletRequest request = keeper.request();
        HttpSession        session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
        }
    }

}
