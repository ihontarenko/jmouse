package org.jmouse.security.web.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.Authentication;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.AuthenticationInspector;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationException;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class AuthorizationFilter implements BeanFilter, AuthenticationInspector {

    private final AuthorizationManager<HttpServletRequest> authorizationManager;

    public AuthorizationFilter(AuthorizationManager<HttpServletRequest> authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AccessResult   accessResult   = authorizationManager.check(authentication, request);

        if (accessResult != null && accessResult.isGranted()) {
            chain.doFilter(request, response);
            return;
        }

        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            throw new AuthenticationException("Full authentication is required to access this resource");
        }

        throw new AuthorizationException("Access is denied");
    }

}