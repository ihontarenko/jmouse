package org.jmouse.security.core;

public class ThreadLocalSecurityContextHolderStrategy implements SecurityContextHolderStrategy {

    private static final ThreadLocal<SecurityContext> HOLDER = ThreadLocal.withInitial(SecurityContext.Context::new);

    @Override
    public SecurityContext getContext() {
        return HOLDER.get();
    }

    @Override
    public void setContext(SecurityContext context) {
        HOLDER.set(context);
    }

    @Override
    public void clearContext() {
        HOLDER.remove();
    }

}
