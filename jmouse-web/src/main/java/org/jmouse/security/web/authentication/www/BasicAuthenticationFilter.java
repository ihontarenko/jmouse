package org.jmouse.security.web.authentication.www;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;

public class BasicAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private AuthenticationProvider authenticationProvider = new BasicAuthenticationProvider();

    public BasicAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, requestMatcher, successHandler, failureHandler);
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
