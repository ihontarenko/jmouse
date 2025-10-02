package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.UserIdentityAuthenticationFilter;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;

public class FormLoginConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, FormLoginConfigurer<B>> {

    private String loginUrl = "/login";

    public FormLoginConfigurer<B> loginProcessingUrl(String url) {
        this.loginUrl = url;
        return this;
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository repository, RequestMatcher matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        return new UserIdentityAuthenticationFilter(
                authenticationManager, repository, matcher, successHandler, failureHandler);
    }

}
