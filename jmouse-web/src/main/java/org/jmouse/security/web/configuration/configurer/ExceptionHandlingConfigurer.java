package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.AuthorizationFailureHandler;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.authentication.ui.LoginUrlAuthenticationEntryPoint;
import org.jmouse.security.web.authorization.ForbiddenAuthorizationFailureHandler;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.web.http.cache.HttpSessionRequestCache;
import org.jmouse.web.http.cache.RequestCache;

public class ExceptionHandlingConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<ExceptionHandlingConfigurer<B>, B> {

    public static final String DEFAULT_LOGIN_URL = "/login";

    private RequestCache                requestCache;
    private AuthenticationEntryPoint    entryPoint;
    private AuthorizationFailureHandler deniedHandler;

    public ExceptionHandlingConfigurer<B> authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
        return this;
    }

    public ExceptionHandlingConfigurer<B> accessDeniedHandler(AuthorizationFailureHandler deniedHandler) {
        this.deniedHandler = deniedHandler;
        return this;
    }

    public ExceptionHandlingConfigurer<B> requestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
        return this;
    }

    @Override
    public void configure(B http) throws Exception {
        if (requestCache == null) {
            requestCache = new HttpSessionRequestCache();
            getBuilder().setSharedObject(SharedAttributes.REQUEST_CACHE, requestCache);
        }

        if (entryPoint == null) {
            entryPoint = new LoginUrlAuthenticationEntryPoint(DEFAULT_LOGIN_URL);
        }

        http.setSharedObject(SharedAttributes.ENTRY_POINT, entryPoint);

        http.addFilter(new ExceptionTranslationFilter(
                requestCache, entryPoint, resolveFailureHandler()));
    }

    private AuthorizationFailureHandler resolveFailureHandler() {
        B                           builder = getBuilder();
        AuthorizationFailureHandler handler = builder.getObject(AuthorizationFailureHandler.class);
        if (handler == null) {
            handler = deniedHandler == null ? defaultFailureHandler() : deniedHandler;
        }
        builder.setSharedObject(SharedAttributes.DENIED_HANDLER, handler);
        return handler;
    }

    private AuthorizationFailureHandler defaultFailureHandler() {
        return new ForbiddenAuthorizationFailureHandler();
    }

}
