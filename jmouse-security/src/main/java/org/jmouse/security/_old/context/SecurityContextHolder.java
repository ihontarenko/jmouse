package org.jmouse.security._old.context;

final public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> THREAD_LOCAL = new InheritableThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static SecurityContext getContext() {
        SecurityContext context = THREAD_LOCAL.get();

        if (context == null) {
            context = SecurityContext.of(null, null);
            THREAD_LOCAL.set(context);
        }

        return context;
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
