package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;

public class UserIdentityAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String JMOUSE_USER_IDENTITY_USERNAME = "username";
    public static final String JMOUSE_USER_IDENTITY_PASSWORD = "password";

    private String usernameParameter = JMOUSE_USER_IDENTITY_USERNAME;
    private String passwordParameter = JMOUSE_USER_IDENTITY_PASSWORD;

    public UserIdentityAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler
    ) {
        super(authenticationManager, contextRepository, requestMatcher, successHandler, failureHandler);
    }

    public UserIdentityAuthenticationFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher
    ) {
        super(authenticationManager, contextRepository, requestMatcher, new NoopHttp200SuccessHandler(), new NoopHttp401FailureHandler());
    }

    @Override
    protected Authentication tryAuthenticate(HttpServletRequest request) throws Exception {
        String username = request.getParameter(getUsernameParameter());
        String password = request.getParameter(getPasswordParameter());

        return null;
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
}
