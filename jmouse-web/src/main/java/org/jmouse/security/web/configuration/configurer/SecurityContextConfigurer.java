package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.core.ThreadLocalSecurityContextHolderStrategy;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SecurityConfigurer;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.ForceEagerSessionCreationFilter;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.security.web.session.SessionSettings;

public class SecurityContextConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private Boolean allowRewriteOverride;

    public SecurityContextConfigurer<B> allowRewrite(boolean allowRewrite) {
        this.allowRewriteOverride = allowRewrite;
        return this;
    }

    @Override
    public void configure(B http) {
        SecurityContextHolderStrategy strategy = http.getSharedObject(SecurityContextHolderStrategy.class);
        if (strategy == null) {
            strategy = new ThreadLocalSecurityContextHolderStrategy();
            http.setSharedObject(SharedAttributes.SECURITY_CONTEXT_HOLDER_STRATEGY, strategy);
        }

        SecurityContextHolder.setContextHolderStrategy(strategy);

        SessionSettings       settings = http.getSharedObject(SessionSettings.class);
        SessionCreationPolicy policy   = (settings != null) ? settings.policy() : SessionCreationPolicy.IF_REQUIRED;

        boolean enableUrlRewriting = (settings != null) && settings.urlRewriting();
        boolean allowRewrite       = (allowRewriteOverride != null) ? allowRewriteOverride : enableUrlRewriting;

        SecurityContextRepository repository = http.getSharedObject(SecurityContextRepository.class);
        if (repository == null) {
            repository = switch (policy) {
                case STATELESS ->
                        new RequestAttributeSecurityContextRepository();
                case IF_PRESENT, IF_REQUIRED, ALWAYS ->
                        new HttpSessionSecurityContextRepository().allowSessionCreation(policy.isAllowCreate());
            };
            http.setSharedObject(SharedAttributes.CONTEXT_REPOSITORY, repository);
        }

        if (policy.isForceEager()) {
            http.addFilter(new ForceEagerSessionCreationFilter());
        }

        http.addFilter(new SecurityContextPersistenceFilter(repository, allowRewrite));
    }

}
