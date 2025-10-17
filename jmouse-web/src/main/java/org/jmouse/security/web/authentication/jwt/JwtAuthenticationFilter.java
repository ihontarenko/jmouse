package org.jmouse.security.web.authentication.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AbstractAuthenticationFilter;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.bearer.BearerTokenAuthenticationProvider;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.match.routing.MatcherCriteria;

/**
 * 🚪 Extracts Bearer token and delegates to AuthenticationManager.
 */
public final class JwtAuthenticationFilter extends AbstractAuthenticationFilter {

    private final BearerTokenAuthenticationProvider provider = new BearerTokenAuthenticationProvider();

    public JwtAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcherCriteria,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, matcherCriteria, successHandler, failureHandler);
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) {
        return provider.provide(request);
    }

}
