package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.ui.LoginUrlAuthenticationEntryPoint;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.cache.HttpSessionRequestCache;
import org.jmouse.web.http.cache.RequestCache;

public class ExceptionHandlingConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<ExceptionHandlingConfigurer<B>, B> {

    public static final String DEFAULT_LOGIN_URL = "/login";

    private RequestCache                 requestCache;
    private AuthenticationEntryPoint     entryPoint;
    private AuthenticationSuccessHandler successHandler;
    private AccessDeniedHandler          deniedHandler;

    public ExceptionHandlingConfigurer<B> authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
        return this;
    }

    public ExceptionHandlingConfigurer<B> accessDeniedHandler(AccessDeniedHandler deniedHandler) {
        this.deniedHandler = deniedHandler;
        return this;
    }

    public ExceptionHandlingConfigurer<B> requestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
        return this;
    }

    public ExceptionHandlingConfigurer<B> authenticationSuccessHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
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

        if (deniedHandler == null) {
            deniedHandler = (request, response, e)
                    -> response.setStatus(HttpStatus.FORBIDDEN.getCode());
        }

        http.setSharedObject(SharedAttributes.ENTRY_POINT, entryPoint);
        http.setSharedObject(SharedAttributes.DENIED_HANDLER, deniedHandler);

        http.addFilter(new ExceptionTranslationFilter(requestCache, entryPoint, deniedHandler));
    }

}
