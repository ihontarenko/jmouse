package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.core.ThreadLocalSecurityContextHolderStrategy;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.security.web.session.SessionSettings;

public class SecurityContextConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<SecurityContextConfigurer<B>, B> {

    @Override
    public void configure(B http) {
        SecurityContextHolderStrategy strategy = http.getSharedObject(SecurityContextHolderStrategy.class);
        if (strategy == null) {
            strategy = new ThreadLocalSecurityContextHolderStrategy();
            http.setSharedObject(SharedAttributes.SECURITY_CONTEXT_HOLDER_STRATEGY, strategy);
        }

        SecurityContextHolder.setContextHolderStrategy(strategy);

        SessionSettings           settings   = http.getSharedObject(SessionSettings.class);
        SessionCreationPolicy     policy     = (settings != null) ? settings.policy() : SessionCreationPolicy.IF_REQUIRED;
        SecurityContextRepository repository = http.getSharedObject(SecurityContextRepository.class);

        if (repository == null) {
            repository = switch (policy) {
                case STATELESS -> new RequestAttributeSecurityContextRepository();
                case IF_PRESENT, IF_REQUIRED, ALWAYS ->
                        new HttpSessionSecurityContextRepository()
                                .allowSessionCreation(policy.isAllowCreate());
            };
            http.setSharedObject(SharedAttributes.CONTEXT_REPOSITORY, repository);
        }

        http.addFilter(new SecurityContextPersistenceFilter(repository));
    }

}
