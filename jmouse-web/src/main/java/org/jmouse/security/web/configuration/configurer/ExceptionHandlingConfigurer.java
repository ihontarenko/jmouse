package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.web.http.HttpStatus;

public class ExceptionHandlingConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<ExceptionHandlingConfigurer<B>, B> {

    private AuthenticationEntryPoint entryPoint;
    private AccessDeniedHandler      deniedHandler;

    public ExceptionHandlingConfigurer<B> authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
        return this;
    }
    public ExceptionHandlingConfigurer<B> accessDeniedHandler(AccessDeniedHandler deniedHandler) {
        this.deniedHandler = deniedHandler;
        return this;
    }

    @Override
    public void configure(B http) throws Exception {
        if (entryPoint == null) {
            entryPoint = (request, response, e)
                    -> response.setStatus(HttpStatus.UNAUTHORIZED.getCode());
        }

        if (deniedHandler == null) {
            deniedHandler = (request, response, e)
                    -> response.setStatus(HttpStatus.FORBIDDEN.getCode());
        }

        http.setSharedObject(SharedAttributes.ENTRY_POINT, entryPoint);
        http.setSharedObject(SharedAttributes.DENIED_HANDLER, deniedHandler);

        http.addFilter(new OrderedFilter(new ExceptionTranslationFilter(entryPoint, deniedHandler), 190));
    }

}
