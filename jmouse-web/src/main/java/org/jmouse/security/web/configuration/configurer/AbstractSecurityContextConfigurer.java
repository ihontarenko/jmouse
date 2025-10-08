package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.HttpSessionSecurityContextRepository;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.security.web.session.SessionSettings;

import static org.jmouse.security.web.session.SessionCreationPolicy.IF_REQUIRED;

public class AbstractSecurityContextConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<SecurityContextConfigurer<B>, B> {

    public AbstractSecurityContextConfigurer<B> contextRepository(SecurityContextRepository repository) {
        getBuilder().setSharedObject(SharedAttributes.CONTEXT_REPOSITORY, repository);
        return this;
    }

    protected SecurityContextRepository resolveSecurityContextRepository(B http) {
        SessionSettings           settings   = http.getSharedObject(SessionSettings.class);
        SessionCreationPolicy     policy     = (settings != null) ? settings.policy() : IF_REQUIRED;
        SecurityContextRepository repository = http.getObject(SecurityContextRepository.class);

        if (repository == null) {
            repository = switch (policy) {
                case STATELESS -> new RequestAttributeSecurityContextRepository();
                case IF_PRESENT, IF_REQUIRED, ALWAYS ->
                        new HttpSessionSecurityContextRepository()
                                .allowSessionCreation(policy.isAllowCreate());
            };
            contextRepository(repository);
        }

        return repository;
    }

}
