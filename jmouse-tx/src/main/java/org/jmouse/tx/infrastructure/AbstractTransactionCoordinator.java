package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.*;
import org.jmouse.tx.synchronization.*;

/**
 * Template-based transaction coordinator.
 */
public abstract class AbstractTransactionCoordinator
        extends SynchronizationSupport implements TransactionManager {

    protected final TransactionContextHolder     contextHolder;
    protected final SynchronizationContextHolder synchronizationHolder;
    protected final TransactionSessionFactory    sessionFactory;

    protected AbstractTransactionCoordinator(
            TransactionContextHolder contextHolder,
            TransactionSessionFactory sessionFactory,
            SynchronizationContextHolder synchronizationHolder
    ) {
        this.contextHolder = contextHolder;
        this.sessionFactory = sessionFactory;
        this.synchronizationHolder = synchronizationHolder;
    }

    @Override
    public final TransactionStatus begin(TransactionDefinition definition) {
        TransactionContext existing = contextHolder.getCurrent();

        if (existing != null) {
            return handleExistingTransaction(definition, existing);
        }

        return startNewTransaction(definition);
    }

    @Override
    public final void commit(TransactionStatus status) {
        if (status.isCompleted()) {
            throw new IllegalStateException("Transaction already completed");
        }

        TransactionContext context = contextHolder.getCurrent();

        if (context == null) {
            throw new IllegalStateException("No transaction context bound");
        }

        if (status.isRollbackOnly()) {
            rollback(status);
            return;
        }

        processCommit(context, status);
    }

    @Override
    public final void rollback(TransactionStatus status) {
        TransactionContext context = contextHolder.getCurrent();
        if (context == null) {
            throw new IllegalStateException("No transaction context bound");
        }

        processRollback(context, status);
    }

    protected TransactionStatus handleExistingTransaction(
            TransactionDefinition definition,
            TransactionContext existing
    ) {
        TransactionPropagation propagation = definition.getPropagation();

        switch (propagation) {
            case REQUIRED:
            case SUPPORTS:
                return participateInExisting(existing);

            case MANDATORY:
                return participateInExisting(existing);

            case NEVER:
                throw new IllegalStateException(
                        "Existing transaction found but propagation is NEVER"
                );

            case REQUIRES_NEW:
                suspend(existing);
                return startNewTransaction(definition);

            case NESTED:
                return startNestedTransaction(definition, existing);

            default:
                throw new IllegalStateException(
                        "Unsupported propagation: " + propagation
                );
        }
    }

    protected TransactionStatus startNewTransaction(
            TransactionDefinition definition
    ) {
        TransactionSession session = doOpenSession(definition);

        TransactionStatusSupport status =
                new TransactionStatusSupport(true);

        doBegin(session, definition);

        MutableTransactionContext context =
                new MutableTransactionContext(definition, status, session);

        contextHolder.bind(context);

        SynchronizationContext synchronizationContext =
                new DefaultSynchronizationContext();

        if (status.isNew()) {
            synchronizationHolder.bind(synchronizationContext);
        }

        triggerBeforeBegin(synchronizationContext);

        return status;
    }

    protected TransactionStatus participateInExisting(
            TransactionContext existing
    ) {
        return existing.getStatus();
    }

    protected TransactionStatus startNestedTransaction(
            TransactionDefinition definition,
            TransactionContext existing
    ) {
        TransactionSession session = existing.getSession();

        if (!(session instanceof SavepointSupport savepointCapable)) {
            throw new IllegalStateException(
                    "Nested transactions require SavepointSupport"
            );
        }

        Object savepoint = savepointCapable.createSavepoint();

        MutableTransactionContext nestedContext =
                new MutableTransactionContext(
                        definition,
                        new TransactionStatusSupport(false),
                        session
                );

        nestedContext.setSavepoint(savepoint);

        contextHolder.bind(nestedContext);

        return nestedContext.getStatus();
    }

    // ------------------------------------------------------------
    // Commit / rollback logic
    // ------------------------------------------------------------

    protected void processCommit(
            TransactionContext context,
            TransactionStatus status
    ) {
        SynchronizationContext synchronizationContext =
                synchronizationHolder.getCurrent();

        try {
            triggerBeforeCommit(synchronizationContext);

            if (status.isNew()) {
                doCommit(context.getSession());
            } else {
                context.getSavepoint().ifPresent(
                        sp -> doReleaseSavepoint(context.getSession(), sp)
                );
            }

            triggerAfterCommit(synchronizationContext);
            triggerAfterCompletion(
                    synchronizationContext,
                    TransactionSynchronization.CompletionStatus.COMMITTED
            );
        } catch (Exception ex) {
            triggerAfterRollback(synchronizationContext);
            triggerAfterCompletion(
                    synchronizationContext,
                    TransactionSynchronization.CompletionStatus.ROLLED_BACK
            );
            throw ex;
        } finally {
            cleanupAfterCompletion(status);
        }
    }


    protected void processRollback(
            TransactionContext context,
            TransactionStatus status
    ) {
        SynchronizationContext syncContext =
                synchronizationHolder.getCurrent();

        try {
            if (status.isNew()) {
                doRollback(context.getSession());
            } else {
                context.getSavepoint().ifPresent(
                        sp -> doRollbackToSavepoint(context.getSession(), sp)
                );
            }

            triggerAfterRollback(syncContext);
            triggerAfterCompletion(
                    syncContext,
                    TransactionSynchronization.CompletionStatus.ROLLED_BACK
            );
        } finally {
            cleanupAfterCompletion(status);
        }
    }


    protected void cleanupAfterCompletion(TransactionStatus status) {
        status.markCompleted();
        contextHolder.clear();
    }

    // ------------------------------------------------------------
    // Suspend / resume
    // ------------------------------------------------------------

    protected void suspend(TransactionContext context) {
        doSuspend(context);
        contextHolder.clear();
    }

    protected void resume(TransactionContext context) {
        contextHolder.bind(context);
        doResume(context);
    }

    // ------------------------------------------------------------
    // Template hooks (do*)
    // ------------------------------------------------------------

    protected abstract TransactionSession doOpenSession(
            TransactionDefinition definition
    );

    protected abstract void doBegin(
            TransactionSession session,
            TransactionDefinition definition
    );

    protected abstract void doCommit(
            TransactionSession session
    );

    protected abstract void doRollback(
            TransactionSession session
    );

    protected abstract void doSuspend(
            TransactionContext context
    );

    protected abstract void doResume(
            TransactionContext context
    );

    protected abstract void doRollbackToSavepoint(
            TransactionSession session,
            Object savepoint
    );

    protected abstract void doReleaseSavepoint(
            TransactionSession session,
            Object savepoint
    );

}
