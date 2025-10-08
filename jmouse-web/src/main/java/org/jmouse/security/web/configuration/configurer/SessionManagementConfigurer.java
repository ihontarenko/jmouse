package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.core.session.SessionRegistry;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.*;
import org.jmouse.web.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public final class SessionManagementConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractSecurityContextConfigurer<B> {

    private boolean                    rewrite = false;
    private SessionCreationPolicy      policy  = SessionCreationPolicy.IF_REQUIRED;
    private SessionAuthenticateHandler sessionAuthentication;
    private SessionInvalidHandler      sessionInvalidHandler;
    private SessionRegistry            sessionRegistry;
    private Integer                    maximumSessions;

    public SessionManagementConfigurer<B> policy(SessionCreationPolicy policy) {
        this.policy = policy;
        return this;
    }

    public SessionManagementConfigurer<B> sessionRegistry(SessionRegistry registry) {
        this.sessionRegistry = registry;
        return this;
    }

    public SessionManagementConfigurer<B> maximumSessions(int maximumSessions) {
        this.maximumSessions = maximumSessions;
        return this;
    }

    public SessionCreationPolicyConfigurer policy() {
        return new SessionCreationPolicyConfigurer();
    }

    public SessionManagementConfigurer<B> rewrite(boolean rewrite) {
        this.rewrite = rewrite;
        return this;
    }

    public URLRewriteConfigurer rewrite() {
        return new URLRewriteConfigurer();
    }

    public SessionManagementConfigurer<B> sessionAuthenticationStrategy(SessionAuthenticateHandler strategy) {
        this.sessionAuthentication = strategy;
        return this;
    }

    public SessionManagementConfigurer<B> invalidSessionStrategy(SessionInvalidHandler strategy) {
        this.sessionInvalidHandler = strategy;
        return this;
    }

    @Override
    public void configure(B http) {
        http.setSharedObject(SessionSettings.class, new SessionSettings(
                policy, rewrite, sessionAuthentication, sessionInvalidHandler));

        if (policy.isForceEager()) {
            http.addFilter(new ForceEagerSessionCreationFilter());
        }

        if (rewrite) {
            http.addFilter(new DisableURLEncodingFilter());
        }

        // 🔹 Build session-auth strategy (existing resolve + concurrency)
        SessionAuthenticateHandler strategy =
                (this.sessionAuthentication != null)
                        ? this.sessionAuthentication
                        : autoDetectStrategy(sessionRegistry);

        // If concurrency configured -> decorate with concurrency auth handler
        if (maximumSessions != null && policy != SessionCreationPolicy.STATELESS) {
            SessionAuthenticateHandler sessionControl = new SessionExpirationAuthenticateHandler(
                    maximumSessions, sessionRegistry
            );
            strategy = new CompositeSessionAuthenticateHandler(
                    new ChangeSessionIdAuthenticateHandler(),
                    (sessionRegistry != null
                            ? new RegisterNewSessionAuthenticateHandler(sessionRegistry)
                            : new NullAuthenticatedSessionStrategy()),
                    sessionControl
            );
        }

        http.addFilter(new SessionManagementFilter(strategy, this.sessionInvalidHandler));

        if (maximumSessions != null && policy != SessionCreationPolicy.STATELESS) {
            SessionInvalidHandler invalid = (this.sessionInvalidHandler != null)
                    ? this.sessionInvalidHandler
                    : new HttpStatusInvalidSessionHandler(HttpStatus.LOGIN_TIME_OUT);
            http.addFilter(new SessionControlFilter(sessionRegistry, invalid));
        }
    }

    private SessionAuthenticateHandler autoDetectStrategy(SessionRegistry registry) {
        List<SessionAuthenticateHandler> chain = new ArrayList<>();

        chain.add(new NullAuthenticatedSessionStrategy());

        if (!policy.isStateless()) {
            chain.add(new ChangeSessionIdAuthenticateHandler());
            if (registry != null) {
                chain.add(new RegisterNewSessionAuthenticateHandler(registry));
            }
        }

        return compose(chain);
    }

    private List<SessionAuthenticateHandler> sessionControlChain(SessionRegistry registry, int maxSessions) {
        List<SessionAuthenticateHandler> chain = new ArrayList<>();
        chain.add(new ChangeSessionIdAuthenticateHandler());
        chain.add(new SessionExpirationAuthenticateHandler(maxSessions, registry)); // твій клас
        chain.add(new RegisterNewSessionAuthenticateHandler(registry));
        return chain;
    }

    private SessionAuthenticateHandler compose(List<SessionAuthenticateHandler> chain) {
        if (chain.isEmpty()) {
            return new NullAuthenticatedSessionStrategy();
        }

        if (chain.size() == 1) {
            return chain.getFirst();
        }

        return new CompositeSessionAuthenticateHandler(chain.toArray(SessionAuthenticateHandler[]::new));
    }

    public final class URLRewriteConfigurer {
        public SessionManagementConfigurer<B> enable() {
            return SessionManagementConfigurer.this.rewrite(true);
        }

        public SessionManagementConfigurer<B> disable() {
            return SessionManagementConfigurer.this.rewrite(false);
        }
    }

    public final class SessionCreationPolicyConfigurer {
        public SessionManagementConfigurer<B> stateless() {
            return SessionManagementConfigurer.this.policy(SessionCreationPolicy.STATELESS);
        }

        public SessionManagementConfigurer<B> always() {
            return SessionManagementConfigurer.this.policy(SessionCreationPolicy.ALWAYS);
        }

        public SessionManagementConfigurer<B> ifPresent() {
            return SessionManagementConfigurer.this.policy(SessionCreationPolicy.IF_PRESENT);
        }

        public SessionManagementConfigurer<B> isRequired() {
            return SessionManagementConfigurer.this.policy(SessionCreationPolicy.IF_REQUIRED);
        }
    }

}
