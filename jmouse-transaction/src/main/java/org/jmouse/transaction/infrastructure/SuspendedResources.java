package org.jmouse.transaction.infrastructure;

import java.util.List;
import java.util.Map;
import org.jmouse.transaction.synchronization.SynchronizationContext;

/** Detached synchronous transaction scope, including all enclosing participation scopes. */
public record SuspendedResources(
    TransactionContext transactionContext,
    SynchronizationContext synchronizationContext,
    Map<Class<?>, Object> resourceSnapshot,
    List<TransactionContext> enclosingContexts,
    List<SynchronizationContext> enclosingSynchronizations
) {
    public SuspendedResources {
        resourceSnapshot = Map.copyOf(resourceSnapshot);
        enclosingContexts = List.copyOf(enclosingContexts);
        enclosingSynchronizations = List.copyOf(enclosingSynchronizations);
    }

    public SuspendedResources(
        TransactionContext transactionContext,
        SynchronizationContext synchronizationContext,
        Map<Class<?>, Object> resourceSnapshot
    ) {
        this(transactionContext, synchronizationContext, resourceSnapshot, List.of(), List.of());
    }
}
