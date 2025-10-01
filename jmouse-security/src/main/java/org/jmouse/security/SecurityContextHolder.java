package org.jmouse.security;

import org.jmouse.security.core.SecurityContext;
import org.jmouse.security.core.SecurityContextHolderStrategy;
import org.jmouse.security.core.ThreadLocalSecurityContextHolderStrategy;

final public class SecurityContextHolder {

    private static volatile SecurityContextHolderStrategy CONTEXT_HOLDER_STRATEGY;

    static {
        setContextHolderStrategy(new ThreadLocalSecurityContextHolderStrategy());
    }

    private SecurityContextHolder() {
    }

    public static SecurityContext getContext() {
        return CONTEXT_HOLDER_STRATEGY.getContext();
    }

    public static void setContext(SecurityContext context) {
        CONTEXT_HOLDER_STRATEGY.setContext(context);
    }

    public static void clearContext() {
        CONTEXT_HOLDER_STRATEGY.clearContext();
    }

    public static SecurityContextHolderStrategy getContextHolderStrategy() {
        return CONTEXT_HOLDER_STRATEGY;
    }

    public static void setContextHolderStrategy(SecurityContextHolderStrategy custom) {
        CONTEXT_HOLDER_STRATEGY = (custom != null) ? custom : new ThreadLocalSecurityContextHolderStrategy();
    }

}
