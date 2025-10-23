package org.jmouse.security;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.method.LogErrorMethodAuthorizationDeniedHandler;
import org.jmouse.security.authorization.method.MethodAuthorizationContext;
import org.jmouse.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.jmouse.security.core.access.Phase;
import org.jmouse.security.core.access.annotation.Authorize;
import org.jmouse.security.core.access.annotation.DeniedHandler;

@DenyAll
@DeniedHandler(LogErrorMethodAuthorizationDeniedHandler.class)
public class UserService {

    @RolesAllowed({"USER", "ROLE_ANONYMOUS"})
    public String upper(String username) {
        return username.toUpperCase();
    }

    @RolesAllowed({"R_ADMIN", "R_OWNER"})
    public String lower(String username) {
        return username.toLowerCase();
    }

    @Authorize(value = "returnValue | length >= 9", phase = Phase.AFTER)
    @DeniedHandler(Handler.class)
    public String nomalize(String username) {
        return username;
    }

    public static class Handler implements MethodAuthorizationDeniedHandler {

        @Override
        public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
            throw new UnsupportedOperationException("Not supported yet." + decision);
        }

    }

}
