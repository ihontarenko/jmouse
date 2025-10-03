package org.jmouse.security.web.session;

import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.session.SessionRegistry;
import org.jmouse.web.http.request.RequestContextKeeper;

import java.util.Comparator;
import java.util.List;

public class SessionControlAuthenticateHandler implements SessionAuthenticateHandler {

    private final int             maxSessions;
    private final SessionRegistry sessionRegistry;

    public SessionControlAuthenticateHandler(int maxSessions, SessionRegistry sessionRegistry) {
        this.maxSessions = maxSessions;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        Object principal = (authentication != null) ? authentication.getPrincipal() : null;
        if (principal != null) {
            List<SessionRegistry.SessionInformation> sessions = sessionRegistry.getSessions(principal);
            if (sessions.size() > maxSessions) {
                int toExpire = sessions.size() - (maxSessions - 1);
                sessions.sort(Comparator.comparing(SessionRegistry.SessionInformation::lastAccess));
                List<SessionRegistry.SessionInformation> expiredSessions = sessions.subList(0, toExpire);
                expiredSessions.forEach(SessionRegistry.SessionInformation::expireSession);
            }
        }
    }

}
