package org.jmouse.security;

public class InheritableThreadLocalSecurityContextHolderStrategy implements SecurityContextHolderStrategy {

    private static final ThreadLocal<SecurityContext> HOLDER = InheritableThreadLocal.withInitial(SecurityContext::empty);

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
