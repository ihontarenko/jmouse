package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;

public class BasicAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public BasicAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, requestMatcher, successHandler, failureHandler);
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) throws Exception {
        return null;
    }

}
