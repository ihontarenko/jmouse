package org.jmouse.security.web.session;

public final class SessionSettings {

    private final SessionCreationPolicy policy;
    private final boolean               urlRewriting;

    private final SessionAuthenticateHandler sessionAuthStrategy;
    private final SessionInvalidHandler      invalidSessionStrategy;

    public SessionSettings(
            SessionCreationPolicy policy,
            boolean urlRewriting,
            SessionAuthenticateHandler sessionAuthStrategy,
            SessionInvalidHandler invalidSessionStrategy
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

    public SessionAuthenticateHandler sessionAuthStrategy() {
        return sessionAuthStrategy;
    }

    public SessionInvalidHandler invalidSessionStrategy() {
        return invalidSessionStrategy;
    }
}
