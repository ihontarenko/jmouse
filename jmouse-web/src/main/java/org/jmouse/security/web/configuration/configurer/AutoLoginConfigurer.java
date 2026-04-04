package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.remember.AutoLoginAuthenticationFilter;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.match.routing.MatcherCriteria;

public class AutoLoginConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, AutoLoginConfigurer<B>> {

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository,
            MatcherCriteria matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        AutoLoginAuthenticationFilter filter = new AutoLoginAuthenticationFilter(
                authenticationManager, repository, matcher, successHandler, failureHandler
        );

        return filter;
    }

}
