package org.jmouse.security.web.authentication;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestContextKeeper;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public abstract class AbstractAuthenticationProcessingFilter implements BeanFilter {

    protected final SecurityContextRepository contextRepository;
    protected final RequestMatcher            requestMatcher;
    protected final AuthenticationManager     authenticationManager;

    protected AuthenticationSuccessHandler successHandler;
    protected AuthenticationFailureHandler failureHandler;

    protected AbstractAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
            RequestMatcher requestMatcher,
            AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler
    ) {
        this.contextRepository = contextRepository;
        this.authenticationManager = authenticationManager;
        this.requestMatcher = requestMatcher;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    protected abstract Authentication tryAuthenticate(HttpServletRequest request) throws Exception;

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        if (!requestMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Authentication       authentication  = tryAuthenticate(request);
            Authentication       result          = authenticationManager.authenticate(authentication);
            SecurityContext      securityContext = SecurityContext.ofAuthentication(result);
            RequestContextKeeper keeper          = RequestContextKeeper.ofRequestContext(requestContext);

            SecurityContextHolder.setContext(securityContext);

            contextRepository.save(securityContext, keeper);
            successHandler.onSuccess(keeper.request(), keeper.response());
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            failureHandler.onFailure(request, response, exception);
        }
    }

    public SecurityContextRepository getContextRepository() {
        return contextRepository;
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

}
