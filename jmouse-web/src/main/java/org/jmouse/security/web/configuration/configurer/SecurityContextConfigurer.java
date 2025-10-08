package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.core.ThreadLocalSecurityContextHolderStrategy;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;
import org.jmouse.security.web.context.SecurityContextRepository;

public class SecurityContextConfigurer<B extends HttpSecurityBuilder<B>> extends AbstractSecurityContextConfigurer<B> {

    @Override
    public void configure(B http) {
        SecurityContextHolderStrategy strategy = http.getSharedObject(SecurityContextHolderStrategy.class);

        if (strategy == null) {
            strategy = new ThreadLocalSecurityContextHolderStrategy();
            http.setSharedObject(SharedAttributes.SECURITY_CONTEXT_HOLDER_STRATEGY, strategy);
        }

        SecurityContextHolder.setContextHolderStrategy(strategy);

        SecurityContextRepository repository = resolveSecurityContextRepository(http);

        http.addFilter(new SecurityContextPersistenceFilter(repository));
    }

}
