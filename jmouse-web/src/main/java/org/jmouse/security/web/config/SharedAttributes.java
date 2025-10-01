package org.jmouse.security.web.config;

import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.core.SecurityContextRepository;
import org.jmouse.security.web.AccessDeniedHandler;
import org.jmouse.security.web.AuthenticationEntryPoint;

public final class SharedAttributes {

    public static final Class<AuthenticationManager>     AUTHENTICATION_MANAGER = AuthenticationManager.class;
    public static final Class<SecurityContextRepository> CONTEXT_REPOSITORY     = SecurityContextRepository.class;
    public static final Class<AuthenticationEntryPoint>  ENTRY_POINT            = AuthenticationEntryPoint.class;
    public static final Class<AccessDeniedHandler>       DENIED_HANDLER         = AccessDeniedHandler.class;

}
