package org.jmouse.security.web.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.web.AuthorizationFilter;
import org.jmouse.security.web.OrderedFilter;

public final class AuthorizeHttpRequestsConfigurer implements SecurityConfigurer<HttpSecurity> {
    private AuthorizationManager<HttpServletRequest> authorizationManager;

    public AuthorizeHttpRequestsConfigurer rules(AuthorizationManager<HttpServletRequest> manager) {
        this.authorizationManager = manager;
        return this;
    }

    @Override
    public void configure(HttpSecurity http) {
        Filter filter = new AuthorizationFilter(authorizationManager);
        http.addFilter(new OrderedFilter(filter, 200));
    }
}