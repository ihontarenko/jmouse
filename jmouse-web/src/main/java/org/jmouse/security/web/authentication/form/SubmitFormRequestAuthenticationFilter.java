package org.jmouse.security.web.authentication.form;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.Authentication;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.match.routing.MatcherCriteria;

public class SubmitFormRequestAuthenticationFilter extends AbstractAuthenticationFilter {

    public static final String JMOUSE_USER_IDENTITY_USERNAME = "username";
    public static final String JMOUSE_USER_IDENTITY_PASSWORD = "password";

    private String usernameParameter = JMOUSE_USER_IDENTITY_USERNAME;
    private String passwordParameter = JMOUSE_USER_IDENTITY_PASSWORD;

    private AuthenticationProvider authenticationProvider;

    public SubmitFormRequestAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, matcher, successHandler, failureHandler);
    }

    public SubmitFormRequestAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            MatcherCriteria matcherCriteria
    ) {
        this(authenticationManager, contextRepository, matcherCriteria,
             new NoopHttp200SuccessHandler(), new NoopHttp401FailureHandler());
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) throws Exception {
        AuthenticationProvider provider = getAuthenticationProvider();

        if (provider == null) {
            provider = new SubmitFormRequestAuthenticationProvider(
                    getUsernameParameter() == null
                            ? JMOUSE_USER_IDENTITY_USERNAME : getUsernameParameter(),
                    getPasswordParameter() == null
                            ? JMOUSE_USER_IDENTITY_PASSWORD : getPasswordParameter()
            );
        }

        return provider.provide(request);
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
