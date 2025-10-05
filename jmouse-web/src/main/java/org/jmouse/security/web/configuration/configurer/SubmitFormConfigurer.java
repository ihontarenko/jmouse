package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.www.BasicAuthenticationProvider;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;

public class SubmitFormConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, SubmitFormConfigurer<B>> {

    private String loginUrl = "/login";

    private AuthenticationProvider authenticationProvider = new BasicAuthenticationProvider();
    private String                 usernameParameter;
    private String                 passwordParameter;

    public SubmitFormConfigurer<B> processingUrl(String url) {
        this.loginUrl = url;
        return this;
    }

    public SubmitFormConfigurer<B> usernamePassword(String usernameParameter, String passwordParameter) {
        return usernameParameter(usernameParameter).passwordParameter(passwordParameter);
    }

    public SubmitFormConfigurer<B> usernameParameter(String usernameParameter) {
        this.usernameParameter = usernameParameter;
        return this;
    }

    public SubmitFormConfigurer<B> passwordParameter(String passwordParameter) {
        this.passwordParameter = passwordParameter;
        return this;
    }

    public SubmitFormConfigurer<B> authenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        return this;
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository repository, RequestMatcher matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        SubmitFormRequestAuthenticationFilter filter = new SubmitFormRequestAuthenticationFilter(
                authenticationManager, repository, matcher, successHandler, failureHandler);

        filter.setAuthenticationProvider(authenticationProvider);
        filter.setUsernameParameter(usernameParameter);
        filter.setPasswordParameter(passwordParameter);

        return filter;
    }

}
