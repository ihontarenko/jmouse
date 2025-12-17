package org.jmouse.tx.synchronization;

public class ThreadBoundSynchronizationContextHolder implements SynchronizationContextHolder {

    private static final ThreadLocal<SynchronizationContext> CONTEXT = new ThreadLocal<>();

    @Override
    public SynchronizationContext getCurrent() {
        return CONTEXT.get();
    }

    @Override
    public void bind(SynchronizationContext context) {
        CONTEXT.set(context);
    }

    @Override
    public void clear() {
        CONTEXT.remove();
    }
}
