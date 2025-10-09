package org.jmouse.security.web.session;

import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.RequestContextKeeper;

import java.util.List;

public class CompositeSessionAuthenticateHandler implements SessionAuthenticateHandler {

    private final List<SessionAuthenticateHandler> delegates;

    public CompositeSessionAuthenticateHandler(SessionAuthenticateHandler... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        delegates.forEach(d -> d.onAuthentication(authentication, keeper));
    }

}
