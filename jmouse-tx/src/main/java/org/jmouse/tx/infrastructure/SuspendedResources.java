package org.jmouse.tx.infrastructure;

import org.jmouse.tx.synchronization.SynchronizationContext;

import java.util.Map;

public record SuspendedResources(
        TransactionContext transactionContext,
        SynchronizationContext synchronizationContext,
        Map<Class<?>, Object> resourceSnapshot
) { }
