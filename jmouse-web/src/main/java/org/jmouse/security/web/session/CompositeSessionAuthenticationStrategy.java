package org.jmouse.security.web.session;

import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.request.RequestContextKeeper;

import java.util.List;

public class CompositeSessionAuthenticationStrategy implements SessionAuthenticationStrategy{

    private final List<SessionAuthenticationStrategy> delegates;

    public CompositeSessionAuthenticationStrategy(SessionAuthenticationStrategy... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public void onAuthentication(Authentication authentication, RequestContextKeeper keeper) {
        delegates.forEach(d -> d.onAuthentication(authentication, keeper));
    }

}
