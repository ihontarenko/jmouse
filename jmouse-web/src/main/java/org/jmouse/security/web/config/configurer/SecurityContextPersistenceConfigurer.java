package org.jmouse.security.web.config.configurer;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.web.context.InMemorySecurityContextRepository;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.core.ThreadLocalSecurityContextHolderStrategy;
import org.jmouse.security.web.config.HttpSecurityBuilder;
import org.jmouse.security.web.config.SecurityConfigurer;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;

public class SecurityContextPersistenceConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private boolean allowRewrite = true;

    public SecurityContextPersistenceConfigurer<B> allowRewrite(boolean allowRewrite) {
        this.allowRewrite = allowRewrite;
        return this;
    }

    @Override
    public void configure(B http) {
        SecurityContextRepository repository = http.getSharedObject(SecurityContextRepository.class);

        if (repository == null) {
            repository = new InMemorySecurityContextRepository();
            http.setSharedObject(SecurityContextRepository.class, repository);
        }

        SecurityContextHolderStrategy strategy = http.getSharedObject(SecurityContextHolderStrategy.class);

        if (strategy == null) {
            strategy = new ThreadLocalSecurityContextHolderStrategy();
            http.setSharedObject(SecurityContextHolderStrategy.class, strategy);
        }

        SecurityContextHolder.setContextHolderStrategy(strategy);

        http.addFilter(new SecurityContextPersistenceFilter(repository, allowRewrite));
    }

}
