package org.jmouse.transaction.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jmouse.core.Verify;
import org.jmouse.transaction.*;
import org.jmouse.transaction.synchronization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template-based transaction coordinator.
 */
public abstract class AbstractTransactionCoordinator
    extends SynchronizationSupport
    implements TransactionManager
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);

    protected final TransactionContextHolder contextHolder;
    protected final SynchronizationContextHolder synchronizationHolder;
    protected final JoinTransactionValidator joinValidator;
    protected final TransactionSessionFactory sessionFactory;

    protected AbstractTransactionCoordinator(
        TransactionContextHolder contextHolder,
        TransactionSessionFactory sessionFactory,
        SynchronizationContextHolder synchronizationHolder,
        JoinTransactionValidator joinValidator
    ) {
        this.contextHolder = contextHolder;
        this.sessionFactory = sessionFactory;
        this.synchronizationHolder = synchronizationHolder;
        this.joinValidator = joinValidator;
    }

    @Override
    public final TransactionStatus begin(TransactionDefinition definition) {
        TransactionContext existing = contextHolder.getContext();
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
            if (!sessionFactory.supportsExistingSession(existing.getSession())) {
                return beginAlongsideForeignTransaction(definition);
            }
            return handleExistingTransaction(definition, existing);
        }

        return startNewTransaction(definition);
    }

    @Override
    public final void commit(TransactionStatus status) {
        if (status instanceof NoopTransactionStatus) {
            return;
        }

        if (
            !status.isNew() &&
            status instanceof TransactionStatusSupport statusSupport &&
            statusSupport.hasSuspended()
        ) {
            if (status.isCompleted() || contextHolder.getContext() != null) {
                throw new IllegalStateException(
                    "Suspended scope must complete after its inner transaction scopes"
                );
            }
            cleanupAfterCompletion(status);
            return;
        }

        if (status.isCompleted()) {
            throw new IllegalStateException("Transaction already completed");
        }

        TransactionContext context = requireCurrentTransaction(status);
        if (
            context instanceof MutableTransactionContext mutable &&
            mutable
                .getAttributes()
                .filter(TransactionAttributes::hasTimeout)
                .filter(attributes -> attributes.deadlineNanos() - System.nanoTime() <= 0)
                .isPresent()
        ) {
            context.setRollbackOnly();
            TransactionTimedOutException failure = new TransactionTimedOutException();
            attempt(failure, () -> rollback(status));
            throw failure;
        }
        if (status.isRollbackOnly()) {
            rollback(status);
            if (status.isNew()) {
                throw new UnexpectedRollbackException();
            }
            return;
        }

        if (!status.isNew() && context.getSavepoint().isEmpty()) {
            cleanupAfterCompletion(status);
            return;
        }
        processCommit(context, status);
    }

    @Override
    public final void rollback(TransactionStatus status) {
        if (status instanceof NoopTransactionStatus) {
            return;
        }

        if (!status.isNew() && status instanceof TransactionStatusSupport ss && ss.hasSuspended()) {
            if (status.isCompleted() || contextHolder.getContext() != null) {
                throw new IllegalStateException(
                    "Suspended scope must complete after its inner transaction scopes"
                );
            }
            cleanupAfterCompletion(status);
            return;
        }

        TransactionContext context = requireCurrentTransaction(status);

        if (!status.isNew() && context.getSavepoint().isEmpty()) {
            context.setRollbackOnly();
            cleanupAfterCompletion(status);
            return;
        }

        processRollback(context, status);
    }

    /** Independent local resources suspend each other; they are never an implicit distributed transaction. */
    private TransactionStatus beginAlongsideForeignTransaction(TransactionDefinition definition) {
        SuspendedResources suspended = suspend();
        try {
            TransactionStatus started = begin(definition);
            TransactionStatusSupport status =
                started instanceof TransactionStatusSupport supported
                    ? supported
                    : new TransactionStatusSupport(false);
            status.setSuspended(suspended);
            return status;
        } catch (RuntimeException | Error failure) {
            try {
                resume(suspended);
            } catch (RuntimeException | Error resumeFailure) {
                if (failure != resumeFailure) {
                    failure.addSuppressed(resumeFailure);
                }
            }
            throw failure;
        }
    }

    protected TransactionStatus handleExistingTransaction(
        TransactionDefinition definition,
        TransactionContext existing
    ) {
        TransactionPropagation propagation = definition.getPropagation();

        switch (propagation) {
            case SUPPORTS, MANDATORY, REQUIRED:
                return participateInExisting(existing, definition);
            case NEVER:
                throw new IllegalStateException("Existing transaction found but propagation is NEVER");
            case REQUIRES_NEW:
                SuspendedResources suspended = suspend();
                try {
                    TransactionStatus status = startNewTransaction(definition);
                    if (status instanceof TransactionStatusSupport statusSupport) {
                        statusSupport.setSuspended(suspended);
                    }
                    return status;
                } catch (RuntimeException | Error failure) {
                    try {
                        resume(suspended);
                    } catch (RuntimeException | Error resumeFailure) {
                        if (failure != resumeFailure) {
                            failure.addSuppressed(resumeFailure);
                        }
                    }
                    throw failure;
                }
            case NESTED:
                return startNestedTransaction(existing);
            case NOT_SUPPORTED:
                TransactionStatusSupport statusSupport = new TransactionStatusSupport(false);
                statusSupport.setSuspended(suspend());
                return statusSupport;
            default:
                throw new IllegalStateException("Unsupported propagation: " + propagation);
        }
    }

    protected TransactionStatus startNewTransaction(TransactionDefinition definition) {
        TransactionSession session = openSession(definition);
        TransactionStatusSupport status = new TransactionStatusSupport(true);
        boolean contextBound = false;
        boolean synchronizationBound = false;
        try {
            session.configure(definition);
            doBegin(session, definition);
            MutableTransactionContext context = new MutableTransactionContext(definition, status, session);
            if (definition.getTimeoutSeconds() > 0) {
                context.setEffectiveTimeoutSeconds(definition.getTimeoutSeconds());
            }
            context.setAttributes(
                new TransactionAttributes(
                    definition.getIsolation(),
                    definition.isReadOnly(),
                    toDeadlineNanos(definition.getTimeoutSeconds())
                )
            );
            contextHolder.bindContext(context);
            contextBound = true;
            synchronizationHolder.bind(new DefaultSynchronizationContext());
            synchronizationBound = true;
            triggerBeforeBegin(synchronizationHolder.getCurrent());
            return status;
        } catch (RuntimeException | Error failure) {
            if (synchronizationBound) {
                attempt(failure, () -> synchronizationHolder.unbind());
            }
            if (contextBound) {
                attempt(failure, () -> contextHolder.unbindContext());
            }
            attempt(failure, session::close);
            status.markCompleted();
            throw failure;
        }
    }

    protected TransactionStatus participateInExisting(
        TransactionContext existing,
        TransactionDefinition requested
    ) {
        if (existing instanceof MutableTransactionContext mutable) {
            mutable.getAttributes().ifPresent(attributes -> joinValidator.validate(requested, attributes));
        }
        TransactionStatus joinedStatus = new ParticipatingStatus(existing.getStatus());
        MutableTransactionContext joined = new MutableTransactionContext(
            existing.getDefinition(),
            joinedStatus,
            existing.getSession()
        );
        if (existing instanceof MutableTransactionContext mutable) {
            mutable.getAttributes().ifPresent(joined::setAttributes);
        }
        contextHolder.bindContext(joined);
        return joinedStatus;
    }

    private static final class ParticipatingStatus extends TransactionStatusSupport {

        private final TransactionStatus owner;

        private ParticipatingStatus(TransactionStatus owner) {
            super(false);
            this.owner = owner;
        }

        @Override
        public boolean isRollbackOnly() {
            return owner.isRollbackOnly();
        }

        @Override
        public void markRollbackOnly() {
            owner.markRollbackOnly();
        }
    }

    protected TransactionStatus startNestedTransaction(TransactionContext outerContext) {
        TransactionSession session = outerContext.getSession();

        if (!(session instanceof SavepointSupport savepointCapable)) {
            throw new IllegalStateException("Nested transactions require SavepointSupport");
        }

        Object savepoint = savepointCapable.createSavepoint();

        MutableTransactionContext nestedContext = new MutableTransactionContext(
            outerContext.getDefinition(),
            new TransactionStatusSupport(false),
            session
        );

        nestedContext.setSavepoint(savepoint);

        if (outerContext instanceof MutableTransactionContext outerMutable) {
            outerMutable.getAttributes().ifPresent(nestedContext::setAttributes);
        }

        contextHolder.bindContext(nestedContext);

        return nestedContext.getStatus();
    }

    // ------------------------------------------------------------
    // Commit / rollback logic
    // ------------------------------------------------------------

    private TransactionContext requireCurrentTransaction(TransactionStatus status) {
        if (status.isCompleted()) {
            throw new IllegalStateException("Transaction already completed");
        }
        TransactionContext context = contextHolder.getContext();
        if (context == null || context.getStatus() != status) {
            throw new IllegalStateException(
                "Transaction must complete in reverse scope order on its owning thread"
            );
        }
        return context;
    }

    protected void processCommit(TransactionContext context, TransactionStatus status) {
        SynchronizationContext synchronizations = synchronizationHolder.getCurrent();
        Throwable failure = null;
        boolean committed = false;
        try {
            if (status.isNew()) {
                triggerBeforeCommit(synchronizations);
                doCommit(context.getSession());
                committed = true;
            } else {
                context.getSavepoint().ifPresent(savepoint -> {
                    doReleaseSavepoint(context.getSession(), savepoint);
                    if (context instanceof MutableTransactionContext mutable) {
                        mutable.clearSavepoint();
                    }
                });
            }
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }

        if (status.isNew()) {
            if (committed) {
                failure = attempt(failure, () -> triggerAfterCommit(synchronizations));
                failure = attempt(failure, () ->
                    triggerAfterCompletion(
                        synchronizations,
                        TransactionSynchronization.CompletionStatus.COMMITTED
                    )
                );
            } else {
                boolean rolledBack = false;
                try {
                    doRollback(context.getSession());
                    rolledBack = true;
                } catch (RuntimeException | Error exception) {
                    failure = append(failure, exception);
                }
                if (rolledBack) {
                    failure = attempt(failure, () -> triggerAfterRollback(synchronizations));
                }
                TransactionSynchronization.CompletionStatus completion = rolledBack
                    ? TransactionSynchronization.CompletionStatus.ROLLED_BACK
                    : TransactionSynchronization.CompletionStatus.UNKNOWN;
                failure = attempt(failure, () -> triggerAfterCompletion(synchronizations, completion));
            }
        }

        boolean failedNested = !status.isNew() && failure != null;
        failure = attempt(failure, () -> cleanupAfterCompletion(status));
        if (failedNested && contextHolder.getContext() != null) {
            contextHolder.getContext().setRollbackOnly();
        }
        rethrow(failure);
    }

    protected void processRollback(TransactionContext context, TransactionStatus status) {
        SynchronizationContext synchronizations = synchronizationHolder.getCurrent();
        Throwable failure = null;
        boolean rolledBack = false;
        try {
            if (status.isNew()) {
                doRollback(context.getSession());
            } else {
                context.getSavepoint().ifPresent(savepoint -> {
                    doRollbackToSavepoint(context.getSession(), savepoint);
                    doReleaseSavepoint(context.getSession(), savepoint);
                    if (context instanceof MutableTransactionContext mutable) {
                        mutable.clearSavepoint();
                    }
                });
            }
            rolledBack = true;
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }
        if (status.isNew()) {
            if (rolledBack) {
                failure = attempt(failure, () -> triggerAfterRollback(synchronizations));
            }
            TransactionSynchronization.CompletionStatus completion = rolledBack
                ? TransactionSynchronization.CompletionStatus.ROLLED_BACK
                : TransactionSynchronization.CompletionStatus.UNKNOWN;
            failure = attempt(failure, () -> triggerAfterCompletion(synchronizations, completion));
        }
        boolean failedNested = !status.isNew() && !rolledBack;
        failure = attempt(failure, () -> cleanupAfterCompletion(status));
        if (failedNested && contextHolder.getContext() != null) {
            contextHolder.getContext().setRollbackOnly();
        }
        rethrow(failure);
    }

    private static Throwable attempt(Throwable failure, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error exception) {
            return append(failure, exception);
        }
        return failure;
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
    }

    protected void cleanupAfterCompletion(TransactionStatus status) {
        TransactionStatusSupport statusSupport =
            status instanceof TransactionStatusSupport support ? support : null;

        if (statusSupport != null) {
            statusSupport.markCompleted();
        }

        TransactionContext finished = contextHolder.unbindContext();

        Throwable cleanupFailure = null;
        try {
            if (status.isNew()) {
                synchronizationHolder.unbind();
                if (finished != null) {
                    finished.getSession().close();
                }
            }
        } catch (RuntimeException | Error failure) {
            cleanupFailure = failure;
            throw failure;
        } finally {
            if (statusSupport != null && statusSupport.hasSuspended()) {
                try {
                    resume(statusSupport.getSuspended());
                } catch (RuntimeException | Error resumeFailure) {
                    if (cleanupFailure == null) {
                        throw resumeFailure;
                    }
                    if (cleanupFailure != resumeFailure) {
                        cleanupFailure.addSuppressed(resumeFailure);
                    }
                } finally {
                    statusSupport.setSuspended(null);
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Suspend / resume
    // ------------------------------------------------------------

    protected SuspendedResources suspend() {
        TransactionContext transactionContext = contextHolder.unbindContext();
        SynchronizationContext synchronizationContext = synchronizationHolder.unbind();
        List<TransactionContext> enclosingContexts = new ArrayList<>();
        List<SynchronizationContext> enclosingSynchronizations = new ArrayList<>();
        TransactionContext enclosing;
        while ((enclosing = contextHolder.unbindContext()) != null) {
            enclosingContexts.add(enclosing);
        }
        SynchronizationContext synchronization;
        while ((synchronization = synchronizationHolder.unbind()) != null) {
            enclosingSynchronizations.add(synchronization);
        }
        Map<Class<?>, Object> snapshot = contextHolder.createSnapshot();
        SuspendedResources suspended = new SuspendedResources(
            transactionContext,
            synchronizationContext,
            snapshot,
            enclosingContexts,
            enclosingSynchronizations
        );
        try {
            doSuspend(transactionContext, snapshot);
        } catch (RuntimeException | Error failure) {
            attempt(failure, () -> resume(suspended));
            throw failure;
        }
        return suspended;
    }

    protected void resume(SuspendedResources suspended) {
        if (suspended != null) {
            contextHolder.applySnapshot(suspended.resourceSnapshot());
            for (TransactionContext context : suspended.enclosingContexts().reversed()) {
                contextHolder.bindContext(context);
            }
            if (suspended.transactionContext() != null) {
                contextHolder.bindContext(suspended.transactionContext());
            }
            for (SynchronizationContext context : suspended.enclosingSynchronizations().reversed()) {
                synchronizationHolder.bind(context);
            }
            if (suspended.synchronizationContext() != null) {
                synchronizationHolder.bind(suspended.synchronizationContext());
            }
            doResume(suspended.transactionContext(), suspended.resourceSnapshot());
        }
    }

    protected TransactionSession openSession(TransactionDefinition definition) {
        return sessionFactory.openSession(definition);
    }

    protected abstract void doBegin(TransactionSession session, TransactionDefinition definition);

    protected abstract void doCommit(TransactionSession session);

    protected abstract void doRollback(TransactionSession session);

    protected abstract void doSuspend(TransactionContext context, Map<Class<?>, Object> snapshot);

    protected abstract void doResume(TransactionContext context, Map<Class<?>, Object> snapshot);

    protected abstract void doRollbackToSavepoint(TransactionSession session, Object savepoint);

    protected abstract void doReleaseSavepoint(TransactionSession session, Object savepoint);

    protected long toDeadlineNanos(int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            return 0L;
        }
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    }
}
