package org.jmouse.transaction.synchronization;

public class DefaultSynchronizationContext implements SynchronizationContext {

    private final SynchronizationRegistry registry = new DefaultSynchronizationRegistry();

    @Override
    public SynchronizationRegistry getRegistry() {
        return registry;
    }

}
