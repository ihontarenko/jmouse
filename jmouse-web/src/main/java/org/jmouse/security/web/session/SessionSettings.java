package org.jmouse.security.web.session;

import static org.jmouse.security.web.session.SessionCreationPolicy.IF_REQUIRED;

public final class SessionSettings {

    private final SessionCreationPolicy policy;
    private final boolean               urlRewriting;

    private final SessionAuthenticationStrategy sessionAuthStrategy;
    private final InvalidSessionStrategy invalidSessionStrategy;

    public SessionSettings(
            SessionCreationPolicy policy,
            boolean urlRewriting,
            SessionAuthenticationStrategy sessionAuthStrategy,
            InvalidSessionStrategy invalidSessionStrategy
    ) {
        this.urlRewriting = urlRewriting;
        this.policy = (policy != null) ? policy : SessionCreationPolicy.IF_REQUIRED;
        this.invalidSessionStrategy = invalidSessionStrategy;
        this.sessionAuthStrategy = sessionAuthStrategy;
    }

    public static SessionSettings of(SessionCreationPolicy policy, boolean rewrite) {
        return new SessionSettings(policy, rewrite, null, null);
    }

    public SessionCreationPolicy policy() {
        return policy;
    }

    public boolean urlRewriting() {
        return urlRewriting;
    }

    public SessionAuthenticationStrategy sessionAuthStrategy() {
        return sessionAuthStrategy;
    }

    public InvalidSessionStrategy invalidSessionStrategy() {
        return invalidSessionStrategy;
    }
}
