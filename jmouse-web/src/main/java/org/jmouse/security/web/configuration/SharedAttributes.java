package org.jmouse.security.web.configuration;

import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;
import org.jmouse.security.web.SecurityFilterOrder;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.cache.RequestCache;

public final class SharedAttributes {

    public static final Class<AuthenticationManager>         AUTHENTICATION_MANAGER           = AuthenticationManager.class;
    public static final Class<SecurityContextRepository>     CONTEXT_REPOSITORY               = SecurityContextRepository.class;
    public static final Class<AuthenticationEntryPoint>      ENTRY_POINT                      = AuthenticationEntryPoint.class;
    public static final Class<AccessDeniedHandler>           DENIED_HANDLER                   = AccessDeniedHandler.class;
    public static final Class<AuthenticationSuccessHandler>  SUCCESS_HANDLER                  = AuthenticationSuccessHandler.class;
    public static final Class<AuthenticationFailureHandler>  FAILURE_HANDLER                  = AuthenticationFailureHandler.class;
    public static final Class<SecurityFilterOrder>           SECURITY_FILTER_ORDER            = SecurityFilterOrder.class;
    public static final Class<SecurityContextHolderStrategy> SECURITY_CONTEXT_HOLDER_STRATEGY = SecurityContextHolderStrategy.class;
    public static final Class<RequestCache>                  REQUEST_CACHE                    = RequestCache.class;

}
