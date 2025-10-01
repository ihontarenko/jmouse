package org.jmouse.security.web.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.UsernameIdentityAuthentication;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class UserIdentityAuthenticationFilter implements BeanFilter {

    public static final String JMOUSE_USER_IDENTITY_USERNAME = "username";
    public static final String JMOUSE_USER_IDENTITY_PASSWORD = "password";

    private final AuthenticationManager        authenticationManager;
    private final RequestMatcher               requestMatcher;
    private       AuthenticationSuccessHandler successHandler    = new NoopHttp200SuccessHandler();
    private       AuthenticationFailureHandler failureHandler    = new NoopHttp401SuccessHandler();
    private       String                       usernameParameter = JMOUSE_USER_IDENTITY_USERNAME;
    private       String                       passwordParameter = JMOUSE_USER_IDENTITY_PASSWORD;

    public UserIdentityAuthenticationFilter(AuthenticationManager authenticationManager, RequestMatcher requestMatcher) {
        this.authenticationManager = authenticationManager;
        this.requestMatcher = requestMatcher;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            if (!requestMatcher.matches(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            String username = request.getParameter(getUsernameParameter());
            String password = request.getParameter(getPasswordParameter());

            try {
                Authentication identityAuthentication = new UsernameIdentityAuthentication(null);
                Authentication authentication         = authenticationManager.authenticate(identityAuthentication);
                authentication.setAuthenticated(true);

                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(authentication);

                successHandler.onSuccess(httpRequest, httpResponse);
            } catch (AuthenticationException authenticationException) {
                failureHandler.onFailure(httpRequest, httpResponse, authenticationException);
            }
        }


        chain.doFilter(request, response);
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }

    public AuthenticationSuccessHandler getSuccessHandler() {
        return successHandler;
    }

    public void setSuccessHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    public AuthenticationFailureHandler getFailureHandler() {
        return failureHandler;
    }

    public void setFailureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
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
