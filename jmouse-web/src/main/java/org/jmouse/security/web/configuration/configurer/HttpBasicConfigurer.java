package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.BasicAuthenticationFilter;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;

public class HttpBasicConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, HttpBasicConfigurer<B>> {

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository repository, RequestMatcher matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        return new BasicAuthenticationFilter(authenticationManager, repository, matcher, successHandler, failureHandler);
    }

}
