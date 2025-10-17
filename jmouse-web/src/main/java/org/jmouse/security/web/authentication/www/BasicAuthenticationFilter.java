package org.jmouse.security.web.authentication.www;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationFilter;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;

public class BasicAuthenticationFilter extends AbstractAuthenticationFilter {

    private AuthenticationProvider authenticationProvider = new BasicAuthenticationProvider();

    public BasicAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcherCriteria,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, matcherCriteria, successHandler, failureHandler);
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) {
        return authenticationProvider.provide(request);
    }

}
