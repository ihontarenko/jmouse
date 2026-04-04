package org.jmouse.security.web.authentication.auto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationFilter;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.io.IOException;

public class AutoLoginAuthenticationFilter extends AbstractAuthenticationFilter {

    private AuthenticationProvider authenticationProvider = new AutoLoginAuthenticationProvider();

    public AutoLoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcherCriteria,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, matcherCriteria, successHandler, failureHandler);
        setContinueChainBeforeSuccess(true);
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) {
        return authenticationProvider.provide(request);
    }

    @Override
    protected void doHandleFailure(
            RequestContext requestContext, FilterChain chain, Exception exception
    ) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        chain.doFilter(requestContext.request(), requestContext.response());
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

}