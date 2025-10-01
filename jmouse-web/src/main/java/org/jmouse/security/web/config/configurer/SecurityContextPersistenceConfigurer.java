package org.jmouse.security.web.config.configurer;

import org.jmouse.security.core.InMemorySecurityContextRepository;
import org.jmouse.security.core.SecurityContextRepository;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.config.HttpSecurityBuilder;
import org.jmouse.security.web.config.SecurityConfigurer;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;

public class SecurityContextPersistenceConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    @Override public void configure(B http) {
        SecurityContextRepository repo =
                http.getSharedObject(SecurityContextRepository.class);
        if (repo == null) repo = new InMemorySecurityContextRepository(); // або session-based

        http.addFilter(new OrderedFilter(
                new SecurityContextPersistenceFilter(repo), 50)); // дуже рано
    }

}
