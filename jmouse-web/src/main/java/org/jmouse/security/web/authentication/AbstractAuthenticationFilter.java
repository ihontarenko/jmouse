package org.jmouse.security.web.authentication;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.Authentication;
import org.jmouse.security.SecurityContext;
import org.jmouse.security.web.SecurityFilter;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.io.IOException;

public abstract class AbstractAuthenticationFilter implements SecurityFilter {

    protected final SecurityContextRepository    contextRepository;
    protected final MatcherCriteria              matcherCriteria;
    protected final AuthenticationManager        authenticationManager;
    protected       AuthenticationSuccessHandler successHandler;
    protected       AuthenticationFailureHandler failureHandler;
    protected       boolean                      continueChainBeforeSuccess = false;

    protected AbstractAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcherCriteria,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        this.contextRepository = contextRepository;
        this.authenticationManager = authenticationManager;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.matcherCriteria = matcherCriteria;
    }

    protected abstract Authentication tryAuthenticate(HttpServletRequest request) throws Exception;

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request      = requestContext.request();
        RequestRoute        requestRoute = RequestRoute.ofRequest(request);
        HttpServletResponse response     = requestContext.response();

        if (!matcherCriteria.matches(requestRoute)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Authentication before = tryAuthenticate(request);

            if (before == null) {
                chain.doFilter(request, response);
                return;
            }

            Authentication after = authenticationManager.authenticate(before);

            if (after != null && after.isAuthenticated()) {
                SecurityContext      securityContext = SecurityContext.ofAuthentication(after);
                RequestContextKeeper keeper          = RequestContextKeeper.ofRequestContext(requestContext);

                SecurityContextHolder.setContext(securityContext);

                contextRepository.save(securityContext, keeper);

                if (continueChainBeforeSuccess) {
                    chain.doFilter(request, response);
                    return;
                }

                successHandler.onSuccess(keeper.request(), keeper.response());
            }

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            failureHandler.onFailure(request, response, exception);
        }
    }

    public void setContinueChainBeforeSuccess(boolean flag) {
        this.continueChainBeforeSuccess = flag;
    }

    public SecurityContextRepository getContextRepository() {
        return contextRepository;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public MatcherCriteria getMatcherCriteria() {
        return matcherCriteria;
    }

    public AuthenticationSuccessHandler getSuccessHandler() {
        return successHandler;
    }

    public void setSuccessHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    public AuthenticationFailureHandler getFailureHandler() {
        return failureHandler;
    }

    public void setFailureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

}
