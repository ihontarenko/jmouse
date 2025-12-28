package org.jmouse.transaction.synchronization;

public abstract class SynchronizationSupport {

    protected abstract SynchronizationContextHolder getSynchronizationContextHolder();

    protected void triggerBeforeBegin(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry()
                    .getSynchronizations()
                    .forEach(TransactionSynchronization::beforeBegin);
        }
    }

    protected void triggerBeforeCommit(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry()
                    .getSynchronizations()
                    .forEach(TransactionSynchronization::beforeCommit);
        }
    }

    protected void triggerAfterCommit(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry()
                    .getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        }
    }

    protected void triggerAfterRollback(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry()
                    .getSynchronizations()
                    .forEach(TransactionSynchronization::afterRollback);
        }
    }

    protected void triggerAfterCompletion(
            SynchronizationContext context,
            TransactionSynchronization.CompletionStatus status
    ) {
        if (context != null) {
            context.getRegistry()
                    .getSynchronizations()
                    .forEach(s -> s.afterCompletion(status));
        }
    }
}
