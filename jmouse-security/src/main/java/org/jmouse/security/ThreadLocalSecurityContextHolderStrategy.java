package org.jmouse.security;

public class ThreadLocalSecurityContextHolderStrategy implements SecurityContextHolderStrategy {

    private static final ThreadLocal<SecurityContext> HOLDER = ThreadLocal.withInitial(SecurityContext::empty);

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

    @Override
    public SecurityContext newContext() {
        return new SecurityContext.Context();
    }

}
