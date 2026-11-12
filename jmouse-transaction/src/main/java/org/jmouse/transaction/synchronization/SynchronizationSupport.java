package org.jmouse.transaction.synchronization;

import java.util.function.Consumer;

public abstract class SynchronizationSupport {

    protected abstract SynchronizationContextHolder getSynchronizationContextHolder();

    protected void triggerBeforeBegin(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry().getSynchronizations().forEach(TransactionSynchronization::beforeBegin);
        }
    }

    protected void triggerBeforeCommit(SynchronizationContext context) {
        if (context != null) {
            context.getRegistry().getSynchronizations().forEach(TransactionSynchronization::beforeCommit);
        }
    }

    protected void triggerAfterCommit(SynchronizationContext context) {
        notifyAfter(context, TransactionSynchronization::afterCommit);
    }

    protected void triggerAfterRollback(SynchronizationContext context) {
        notifyAfter(context, TransactionSynchronization::afterRollback);
    }

    protected void triggerAfterCompletion(
        SynchronizationContext context,
        TransactionSynchronization.CompletionStatus status
    ) {
        notifyAfter(context, synchronization -> synchronization.afterCompletion(status));
    }

    /** Completion is a notification: one failing observer must not prevent the remaining cleanup observers. */
    private void notifyAfter(
        SynchronizationContext context,
        Consumer<TransactionSynchronization> notification
    ) {
        if (context == null) {
            return;
        }
        Throwable failure = null;
        for (TransactionSynchronization synchronization : context.getRegistry().getSynchronizations()) {
            try {
                notification.accept(synchronization);
            } catch (RuntimeException | Error exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
    }
}
