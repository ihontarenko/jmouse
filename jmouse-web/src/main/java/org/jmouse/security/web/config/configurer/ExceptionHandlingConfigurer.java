package org.jmouse.security.web.config.configurer;

import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.config.HttpSecurityBuilder;
import org.jmouse.security.web.config.SecurityConfigurer;
import org.jmouse.security.web.config.SharedAttributes;

public class ExceptionHandlingConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private AuthenticationEntryPoint entryPoint;
    private AccessDeniedHandler      deniedHandler;

    public ExceptionHandlingConfigurer<B> authenticationEntryPoint(AuthenticationEntryPoint ep) {
        this.entryPoint = ep; return this;
    }
    public ExceptionHandlingConfigurer<B> accessDeniedHandler(AccessDeniedHandler dh) {
        this.deniedHandler = dh; return this;
    }

    @Override
    public void configure(B http) throws Exception {
        if (entryPoint == null) {
            entryPoint = (request, response, e)
                    -> response.setStatus(401);
        }

        if (deniedHandler == null) {
            deniedHandler = (request, response, e)
                    -> response.setStatus(403);
        }

        http.setSharedObject(SharedAttributes.ENTRY_POINT, entryPoint);
        http.setSharedObject(SharedAttributes.DENIED_HANDLER, deniedHandler);
    }

}
