package org.jmouse.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class AuthorizationFilter implements BeanFilter {

    private final AuthorizationManager<HttpServletRequest> authorizationManager;

    public AuthorizationFilter(AuthorizationManager<HttpServletRequest> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    /**
     * ⚡ Implement filter logic with type-safe {@link RequestContext}.
     *
     * @param requestContext wrapper around HTTP request/response
     * @param chain          filter chain to continue processing
     */
    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(requestContext.request(), requestContext.response());
    }

}
