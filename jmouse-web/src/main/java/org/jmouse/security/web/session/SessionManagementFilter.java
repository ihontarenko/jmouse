package org.jmouse.security.web.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestContextKeeper;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public final class SessionManagementFilter implements BeanFilter {

    private final SessionAuthenticateHandler sessionAuthentication;
    private final SessionInvalidHandler      invalidSessionHandler;

    public SessionManagementFilter(
            SessionAuthenticateHandler sessionAuthentication, SessionInvalidHandler invalidSessionHandler) {
        this.sessionAuthentication = sessionAuthentication;
        this.invalidSessionHandler = invalidSessionHandler;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        RequestContextKeeper keeper  = RequestContextKeeper.ofRequestContext(requestContext);
        HttpServletRequest   request = requestContext.request();

        if (invalidSessionHandler != null && request.isRequestedSessionIdFromCookie() && !request.isRequestedSessionIdValid()) {
            invalidSessionHandler.onInvalidSession(keeper);
            return;
        }

        boolean beforeAuthenticated = isAuthenticated(SecurityContextHolder.getContext());

        chain.doFilter(requestContext.request(), requestContext.response());

        if (sessionAuthentication != null) {
            SecurityContext after              = SecurityContextHolder.getContext();
            boolean         afterAuthenticated = isAuthenticated(after);

            if (!beforeAuthenticated && afterAuthenticated) {
                Authentication authentication = after.getAuthentication();
                if (authentication != null) {
                    sessionAuthentication.onAuthentication(authentication, keeper);
                }
            }
        }
    }

    private boolean isAuthenticated(SecurityContext securityContext) {
        if (securityContext != null) {
            Authentication authentication = securityContext.getAuthentication();
            return authentication != null && authentication.isAuthenticated();
        }
        return false;
    }
}