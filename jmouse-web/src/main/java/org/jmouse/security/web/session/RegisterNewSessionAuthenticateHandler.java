package org.jmouse.security.web.session;

import jakarta.servlet.http.HttpSession;
import org.jmouse.security.Authentication;
import org.jmouse.security.session.SessionRegistry;
import org.jmouse.web.http.RequestContextKeeper;

public class RegisterNewSessionAuthenticateHandler implements SessionAuthenticateHandler {

    private final SessionRegistry sessionRegistry;

    public RegisterNewSessionAuthenticateHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        HttpSession session = keeper.request().getSession();
        sessionRegistry.registerSession(session.getId(), authentication.getPrincipal());
    }

}
