package org.jmouse.security.web.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authorization.AccessDeniedException;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class AuthorizationFilter implements BeanFilter {

    private final AuthorizationManager<HttpServletRequest> authorizationManager;
    private final AuthenticationEntryPoint                 entryPoint;
    private final AccessDeniedHandler                      accessDeniedHandler;

    public AuthorizationFilter(
            AuthorizationManager<HttpServletRequest> authorizationManager,
            AuthenticationEntryPoint entryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) {
        this.authorizationManager = authorizationManager;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AccessResult   decision       = authorizationManager.check(authentication, request);

cd jmo          if (decision != null && !decision.isGranted()) {
            boolean unauthenticated = (authentication == null || !authentication.isAuthenticated());
            if (unauthenticated) {
                throw new AuthenticationException("Full authentication is required to access this resource");
            } else {
                throw new AccessDeniedException("Access is denied");
            }
        }

        chain.doFilter(request, response);
    }

}
