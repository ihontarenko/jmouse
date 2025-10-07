package org.jmouse.security.web.authentication.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.bearer.BearerTokenAuthenticationProvider;
import org.jmouse.security.web.context.SecurityContextRepository;

/**
 * 🚪 Extracts Bearer token and delegates to AuthenticationManager.
 */
public final class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final BearerTokenAuthenticationProvider provider = new BearerTokenAuthenticationProvider();

    public JwtAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, requestMatcher, successHandler, failureHandler);
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) {
        return provider.provide(request);
    }

}
