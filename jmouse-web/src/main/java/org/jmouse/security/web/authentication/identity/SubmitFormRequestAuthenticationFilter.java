package org.jmouse.security.web.authentication.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.authentication.www.BasicAuthenticationProvider;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;

public class SubmitFormRequestAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String JMOUSE_USER_IDENTITY_USERNAME = "username";
    public static final String JMOUSE_USER_IDENTITY_PASSWORD = "password";

    private String usernameParameter = JMOUSE_USER_IDENTITY_USERNAME;
    private String passwordParameter = JMOUSE_USER_IDENTITY_PASSWORD;

    private AuthenticationProvider authenticationProvider = new BasicAuthenticationProvider();

    public SubmitFormRequestAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, requestMatcher, successHandler, failureHandler);
    }

    public SubmitFormRequestAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher
    ) {
        super(authenticationManager, contextRepository, requestMatcher, new NoopHttp200SuccessHandler(), new NoopHttp401FailureHandler());
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) throws Exception {
        return getAuthenticationManager().authenticate(authenticationProvider.provide(request));
    }

    public String getUsernameParameter() {
        return usernameParameter;
    }

    public void setUsernameParameter(String usernameParameter) {
        this.usernameParameter = usernameParameter;
    }

    public String getPasswordParameter() {
        return passwordParameter;
    }

    public void setPasswordParameter(String passwordParameter) {
        this.passwordParameter = passwordParameter;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }
}
