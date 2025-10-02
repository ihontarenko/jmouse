package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.*;

public final class SessionManagementConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<SessionManagementConfigurer<B>, B> {

    private SessionCreationPolicy policy     = SessionCreationPolicy.IF_REQUIRED;
    private boolean               urlRewrite = false;

    private SessionAuthenticateHandler sessionAuthentication;
    private SessionInvalidHandler      sessionInvalidHandler;

    public SessionManagementConfigurer<B> policy(SessionCreationPolicy policy) {
        this.policy = policy;
        return this;
    }

    public SessionCreationPolicyConfigurer policy() {
        return new SessionCreationPolicyConfigurer();
    }

    public SessionManagementConfigurer<B> urlRewrite(boolean urlRewrite) {
        this.urlRewrite = urlRewrite;
        return this;
    }

    public SessionManagementConfigurer<B> enableUrlRewrite() {
        return urlRewrite(true);
    }

    public SessionManagementConfigurer<B> disableUrlRewrite() {
        return urlRewrite(false);
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
                policy, urlRewrite, sessionAuthentication, sessionInvalidHandler));

        SecurityContextRepository existing = http.getSharedObject(SecurityContextRepository.class);

        if (existing == null) {
            SecurityContextRepository repository = switch (policy) {
                case STATELESS -> new RequestAttributeSecurityContextRepository();
                case IF_PRESENT, IF_REQUIRED, ALWAYS -> new HttpSessionSecurityContextRepository()
                        .allowSessionCreation(policy.isAllowCreate());
            };
            http.setSharedObject(SecurityContextRepository.class, repository);
        }

        if (policy.isForceEager()) {
            http.addFilter(new ForceEagerSessionCreationFilter());
        }

        SessionAuthenticateHandler strategy =
                (this.sessionAuthentication != null) ? this.sessionAuthentication : new ChangeSessionIdAuthenticateHandler();

        http.addFilter(new SessionManagementFilter(strategy, this.sessionInvalidHandler));
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
