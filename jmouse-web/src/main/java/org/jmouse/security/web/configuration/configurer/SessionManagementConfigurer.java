package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jmouse.core.Customizer;
import org.jmouse.http.HttpStatus;
import org.jmouse.security.session.SessionRegistry;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.session.*;

public final class SessionManagementConfigurer<B extends HttpSecurityBuilder<B>> extends AbstractSecurityContextConfigurer<B> {

    private boolean rewrite = false;
    private SessionCreationPolicy policy = SessionCreationPolicy.IF_REQUIRED;
    private SessionAuthenticateHandler sessionAuthentication;
    private SessionInvalidHandler sessionInvalidHandler;
    private SessionRegistry sessionRegistry;
    private Integer maximumSessions;

    public SessionManagementConfigurer<B> policy(SessionCreationPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
        return this;
    }

    public SessionManagementConfigurer<B> sessionRegistry(SessionRegistry registry) {
        this.sessionRegistry = registry;
        return this;
    }

    public SessionManagementConfigurer<B> maximumSessions(int maximumSessions) {
        if (maximumSessions <= 0) {
            throw new IllegalArgumentException("maximumSessions must be positive");
        }
        this.maximumSessions = maximumSessions;
        return this;
    }

    public SessionManagementConfigurer<B> policy(Customizer<SessionCreationPolicyConfigurer> customizer) {
        customizer.customize(new SessionCreationPolicyConfigurer());
        return this;
    }

    public SessionManagementConfigurer<B> rewrite(boolean rewrite) {
        this.rewrite = rewrite;
        return this;
    }

    public SessionManagementConfigurer<B> rewrite(Customizer<URLRewriteConfigurer> customizer) {
        customizer.customize(new URLRewriteConfigurer());
        return this;
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
    public void initialize(B http) {
        http.setSharedObject(
            SessionSettings.class,
            new SessionSettings(policy, rewrite, sessionAuthentication, sessionInvalidHandler)
        );
    }

    @Override
    public void configure(B http) {
        if (policy.isStateless() && maximumSessions != null) {
            throw new IllegalStateException("Concurrent session limits require a stateful session policy");
        }
        if (maximumSessions != null && sessionRegistry == null) {
            sessionRegistry = http.getObject(SessionRegistry.class, SessionRegistry::inMemory);
        }
        http.setSharedObject(
            SessionSettings.class,
            new SessionSettings(policy, rewrite, sessionAuthentication, sessionInvalidHandler)
        );
        if (policy.isForceEager()) {
            http.addFilter(new ForceEagerSessionCreationFilter());
        }
        if (!rewrite) {
            http.addFilter(new DisableURLEncodingFilter());
        }

        SessionAuthenticateHandler strategy =
            sessionAuthentication != null
                ? sessionAuthentication
                : policy.isStateless()
                  ? new NullAuthenticatedSessionStrategy()
                  : new ChangeSessionIdAuthenticateHandler();
        if (sessionRegistry != null && !policy.isStateless()) {
            SessionRegistry registry = sessionRegistry;
            SessionAuthenticateHandler authenticate = strategy;
            RegisterNewSessionAuthenticateHandler register = new RegisterNewSessionAuthenticateHandler(
                registry,
                maximumSessions,
                policy.isAllowCreate()
            );
            strategy = (authentication, keeper) -> {
                HttpSession previous = keeper.request().getSession(false);
                String previousId = previous == null ? null : previous.getId();
                authenticate.onAuthentication(authentication, keeper);
                HttpSession current = keeper.request().getSession(false);
                if (previousId != null && (current == null || !previousId.equals(current.getId()))) {
                    registry.removeSession(previousId);
                }
                register.onAuthentication(authentication, keeper);
            };
            http.setSharedObject(SessionRegistry.class, registry);
            SessionInvalidHandler invalid =
                sessionInvalidHandler != null
                    ? sessionInvalidHandler
                    : new HttpStatusInvalidSessionHandler(HttpStatus.LOGIN_TIME_OUT);
            http.addFilter(new SessionControlFilter(registry, invalid));
        }
        http.setSharedObject(SessionAuthenticateHandler.class, strategy);
        http.addFilter(new SessionManagementFilter(strategy, sessionInvalidHandler));
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
