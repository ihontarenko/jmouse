package org.jmouse.transaction.infrastructure;

import org.jmouse.transaction.synchronization.SynchronizationContext;

import java.util.Map;

public record SuspendedResources(
        TransactionContext transactionContext,
        SynchronizationContext synchronizationContext,
        Map<Class<?>, Object> resourceSnapshot
) { }
