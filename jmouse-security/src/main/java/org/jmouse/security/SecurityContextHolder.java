package org.jmouse.security;

import org.jmouse.security.core.SecurityContext;

final public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> THREAD_LOCAL = new InheritableThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static SecurityContext getContext() {
        return THREAD_LOCAL.get();
    }

    public static void setContext(SecurityContext context) {
        if (context == null) {
            clearContext();
        } else {
            THREAD_LOCAL.set(context);
        }
    }

    public static void clearContext() {
        THREAD_LOCAL.remove();
    }

}
