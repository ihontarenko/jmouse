package org.jmouse.security.web.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authorization.AuthorizationException;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class AuthorizationFilter implements BeanFilter {

    private final AuthorizationManager<HttpServletRequest> authorizationManager;

    public AuthorizationFilter(AuthorizationManager<HttpServletRequest> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request        = requestContext.request();
        HttpServletResponse response       = requestContext.response();
        Authentication      authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("Full authentication is required to access this resource");
        }

        AccessResult decision = authorizationManager.check(authentication, request);

        if (decision != null && !decision.isGranted()) {
            throw new AuthorizationException("Access is denied");
        }

        chain.doFilter(request, response);
    }

}
