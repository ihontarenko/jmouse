package org.jmouse.security.web.config;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.web.AuthorizationFilter;
import org.jmouse.security.web.OrderedFilter;

public final class AuthorizeHttpRequestsConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {
    private AuthorizationManager<HttpServletRequest> authorizationManager;

    public AuthorizeHttpRequestsConfigurer<B> rules(AuthorizationManager<HttpServletRequest> manager) {
        this.authorizationManager = manager;
        return this;
    }

    @Override
    public void configure(B builder) {
        builder.addFilter(new OrderedFilter(new AuthorizationFilter(authorizationManager), 200));
    }
}