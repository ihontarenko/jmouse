package org.jmouse.tx.synchronization;

public abstract class SynchronizationSupport {

    protected abstract SynchronizationContextHolder getSynchronizationContextHolder();

    protected void triggerBeforeBegin(SynchronizationContext context) {
        context.getRegistry()
                .getSynchronizations()
                .forEach(TransactionSynchronization::beforeBegin);
    }

    protected void triggerBeforeCommit(SynchronizationContext context) {
        context.getRegistry()
                .getSynchronizations()
                .forEach(TransactionSynchronization::beforeCommit);
    }

    protected void triggerAfterCommit(SynchronizationContext context) {
        context.getRegistry()
                .getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    protected void triggerAfterRollback(SynchronizationContext context) {
        context.getRegistry()
                .getSynchronizations()
                .forEach(TransactionSynchronization::afterRollback);
    }

    protected void triggerAfterCompletion(
            SynchronizationContext context,
            TransactionSynchronization.CompletionStatus status
    ) {
        context.getRegistry()
                .getSynchronizations()
                .forEach(s -> s.afterCompletion(status));

        getSynchronizationContextHolder().clear();
    }
}
