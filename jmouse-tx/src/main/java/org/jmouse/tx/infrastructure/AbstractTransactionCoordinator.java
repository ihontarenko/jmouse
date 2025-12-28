package org.jmouse.tx.infrastructure;

import org.jmouse.core.Contract;
import org.jmouse.tx.core.*;
import org.jmouse.tx.synchronization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Template-based transaction coordinator.
 */
public abstract class AbstractTransactionCoordinator
        extends SynchronizationSupport implements TransactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);

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
        TransactionContext     existing    = contextHolder.getContext();
        TransactionPropagation propagation = definition.getPropagation();

        if (existing == null) {
            if (propagation == TransactionPropagation.MANDATORY) {
                throw new IllegalStateException("No existing transaction found for propagation MANDATORY");
            }
            if (propagation == TransactionPropagation.NEVER) {
                return NoopTransactionStatus.INSTANCE;
            }
            if (propagation == TransactionPropagation.SUPPORTS) {
                return NoopTransactionStatus.INSTANCE;
            }
            if (propagation == TransactionPropagation.NOT_SUPPORTED) {
                return NoopTransactionStatus.INSTANCE;
            }
        }

        if (existing != null) {
            return handleExistingTransaction(definition, existing);
        }

        return startNewTransaction(definition);
    }

    @Override
    public final void commit(TransactionStatus status) {
        if (status instanceof NoopTransactionStatus) {
            return;
        }

        if (status instanceof TransactionStatusSupport statusSupport && statusSupport.hasSuspended()) {
            cleanupAfterCompletion(status);
            return;
        }

        if (status.isCompleted()) {
            throw new IllegalStateException("Transaction already completed");
        }

        TransactionContext context = contextHolder.getContext();

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
        if (status instanceof NoopTransactionStatus) {
            return;
        }

        if (status instanceof TransactionStatusSupport ss && ss.hasSuspended()) {
            cleanupAfterCompletion(status);
            return;
        }

        TransactionContext context = Contract.nonNull(
                contextHolder.getContext(), "No required transaction context bound.");

        if (!status.isNew() && context.getSavepoint().isEmpty()) {
            context.setRollbackOnly();
            return;
        }

        processRollback(context, status);
    }

    protected TransactionStatus handleExistingTransaction(
            TransactionDefinition definition,
            TransactionContext existing
    ) {
        TransactionPropagation propagation = definition.getPropagation();

        switch (propagation) {
            case SUPPORTS, MANDATORY, REQUIRED:
                return participateInExisting(existing);

            case NEVER:
                throw new IllegalStateException(
                        "Existing transaction found but propagation is NEVER"
                );

            case REQUIRES_NEW:
                SuspendedResources suspended = suspend();
                TransactionStatus status = startNewTransaction(definition);

                if (status instanceof TransactionStatusSupport statusSupport) {
                    statusSupport.setSuspended(suspended);
                }

                return status;

            case NESTED:
                return startNestedTransaction(existing);

            case NOT_SUPPORTED:
                TransactionStatusSupport statusSupport = new TransactionStatusSupport(false);
                statusSupport.setSuspended(suspend());
                return statusSupport;

            default:
                throw new IllegalStateException(
                        "Unsupported propagation: " + propagation
                );
        }
    }

    protected TransactionStatus startNewTransaction(TransactionDefinition definition) {
        TransactionSession       session = openSession(definition);
        TransactionStatusSupport status  = new TransactionStatusSupport(true);

        doBegin(session, definition);

        MutableTransactionContext context                = new MutableTransactionContext(status, session);
        SynchronizationContext    synchronizationContext = new DefaultSynchronizationContext();

        contextHolder.bindContext(context);
        synchronizationHolder.bind(synchronizationContext);

        triggerBeforeBegin(synchronizationHolder.getCurrent());

        return status;
    }

    protected TransactionStatus participateInExisting(
            TransactionContext existing
    ) {
        return existing.getStatus();
    }

    protected TransactionStatus startNestedTransaction(
            TransactionContext existing
    ) {
        // no session
        TransactionSession session = existing.getSession();

        if (!(session instanceof SavepointSupport savepointCapable)) {
            throw new IllegalStateException(
                    "Nested transactions require SavepointSupport"
            );
        }

        Object savepoint = savepointCapable.createSavepoint();

        MutableTransactionContext nestedContext =
                new MutableTransactionContext(
                        new TransactionStatusSupport(false),
                        session
                );

        nestedContext.setSavepoint(savepoint);

        contextHolder.bindContext(nestedContext);

        return nestedContext.getStatus();
    }

    // ------------------------------------------------------------
    // Commit / rollback logic
    // ------------------------------------------------------------

    protected void processCommit(TransactionContext context, TransactionStatus status) {
        SynchronizationContext synchronizationContext = synchronizationHolder.getCurrent();

        try {
            triggerBeforeCommit(synchronizationContext);

            if (status.isNew()) {
                doCommit(context.getSession());
            } else {
                context.getSavepoint().ifPresent(savepoint -> {
                    doReleaseSavepoint(context.getSession(), savepoint);
                    if (context instanceof MutableTransactionContext mutable) {
                        mutable.clearSavepoint();
                    }
                });
            }

            triggerAfterCommit(synchronizationContext);
            triggerAfterCompletion(
                    synchronizationContext,
                    TransactionSynchronization.CompletionStatus.COMMITTED
            );
        } catch (Exception exception) {
            if (status.isNew()) {
                try {
                    doRollback(context.getSession());
                } catch (Exception rollbackException) {
                    LOGGER.error("Failed to rollback transaction!", rollbackException);
                }
            }
            triggerAfterRollback(synchronizationContext);
            triggerAfterCompletion(
                    synchronizationContext,
                    TransactionSynchronization.CompletionStatus.ROLLED_BACK
            );
            throw exception;
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    protected void processRollback(TransactionContext context, TransactionStatus status) {
        SynchronizationContext synchronizationContext = synchronizationHolder.getCurrent();

        try {
            if (status.isNew()) {
                doRollback(context.getSession());
            } else {
                context.getSavepoint().ifPresent(savepoint -> {
                    doRollbackToSavepoint(context.getSession(), savepoint);
                    if (context instanceof MutableTransactionContext mutable) {
                        mutable.clearSavepoint();
                    }
                });
            }

            triggerAfterRollback(synchronizationContext);
            triggerAfterCompletion(
                    synchronizationContext,
                    TransactionSynchronization.CompletionStatus.ROLLED_BACK
            );
        } finally {
            cleanupAfterCompletion(status);
        }
    }

    protected void cleanupAfterCompletion(TransactionStatus status) {
        TransactionStatusSupport statusSupport =
                (status instanceof TransactionStatusSupport support) ? support : null;

        if (statusSupport != null) {
            statusSupport.markCompleted();
        }

        TransactionContext finished = contextHolder.unbindContext();

        if (status.isNew()) {
            synchronizationHolder.unbind();
            if (finished != null) {
                finished.getSession().close();
            }
        }

        if (statusSupport != null && statusSupport.hasSuspended()) {
            resume(statusSupport.getSuspended());
            statusSupport.setSuspended(null);
        }
    }

    // ------------------------------------------------------------
    // Suspend / resume
    // ------------------------------------------------------------

    protected SuspendedResources suspend() {
        TransactionContext     transactionContext     = contextHolder.unbindContext();
        SynchronizationContext synchronizationContext = synchronizationHolder.unbind();
        Map<Class<?>, Object>  snapshot               = contextHolder.createSnapshot();
        doSuspend(transactionContext, snapshot);
        return new SuspendedResources(transactionContext, synchronizationContext, snapshot);
    }

    protected void resume(SuspendedResources suspended) {
        if (suspended != null) {
            contextHolder.bindContext(suspended.transactionContext());
            if (suspended.synchronizationContext() != null) {
                synchronizationHolder.bind(suspended.synchronizationContext());
            }
            contextHolder.applySnapshot(suspended.resourceSnapshot());
            doResume(suspended.transactionContext(), suspended.resourceSnapshot());
        }
    }

    protected TransactionSession openSession(TransactionDefinition definition) {
        return sessionFactory.openSession(definition);
    }

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
            TransactionContext context, Map<Class<?>, Object>  snapshot
    );

    protected abstract void doResume(
            TransactionContext context, Map<Class<?>, Object>  snapshot
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
