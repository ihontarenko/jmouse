package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SecurityConfigurer;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.*;

public final class SessionManagementConfigurer<B extends HttpSecurityBuilder<B>>
        implements SecurityConfigurer<B> {

    private SessionCreationPolicy policy     = SessionCreationPolicy.IF_REQUIRED;
    private boolean               urlRewrite = false;

    private SessionAuthenticationStrategy sessionAuthentication;
    private InvalidSessionStrategy        invalidSession;

    public SessionManagementConfigurer<B> sessionCreationPolicy(SessionCreationPolicy policy) {
        this.policy = (policy != null) ? policy : SessionCreationPolicy.IF_REQUIRED;
        return this;
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

    public SessionManagementConfigurer<B> sessionAuthenticationStrategy(SessionAuthenticationStrategy strategy) {
        this.sessionAuthentication = strategy;
        return this;
    }

    public SessionManagementConfigurer<B> invalidSessionStrategy(InvalidSessionStrategy strategy) {
        this.invalidSession = strategy;
        return this;
    }

    @Override
    public void configure(B http) {
        http.setSharedObject(SessionSettings.class, new SessionSettings(
                policy, urlRewrite, sessionAuthentication, invalidSession));

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

        SessionAuthenticationStrategy strategy =
                (this.sessionAuthentication != null) ? this.sessionAuthentication : new ChangeSessionIdAuthenticationStrategy();

        http.addFilter(new SessionManagementFilter(strategy, this.invalidSession));
    }
}
