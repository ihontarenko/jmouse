package org.jmouse.tx.synchronization;

/**
 * Strategy for binding synchronization context.
 */
public interface SynchronizationContextHolder {

    SynchronizationContext getCurrent();

    void bind(SynchronizationContext context);

    void clear();

}