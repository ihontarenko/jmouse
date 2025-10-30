package org.jmouse.tx;

import java.util.List;
import java.util.Objects;

/**
 * 🧠 Base transaction manager with propagation logic (no XA).
 */
public abstract class AbstractTransactionManager implements TransactionManager {

    @Override
    public final TransactionStatus begin(TransactionDefinition def) {
        Objects.requireNonNull(def, "definition");

        TransactionStatus current = TransactionContext.current();

        // 1) if there's already a tx in this thread
        if (current != null) {
            return handleExisting(def, current);
        }

        // 2) no current tx
        return startNew(def, null);
    }

    private TransactionStatus handleExisting(TransactionDefinition def, TransactionStatus current) {
        return switch (def.getPropagation()) {
            case TransactionDefinition.PROPAGATION_REQUIRED,
                 TransactionDefinition.PROPAGATION_SUPPORTS,
                 TransactionDefinition.PROPAGATION_MANDATORY -> {
                // join existing
                yield current;
            }
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> {
                // suspend current, start new
                Object suspended = suspend(current);
                yield startNew(def, suspended);
            }
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> {
                // suspend and return 'no tx'
                Object suspended2 = suspend(current);
                // represent "no tx" as status with isNew=false and resource=null
                TransactionStatus.Simple s = new TransactionStatus.Simple(false, null, suspended2);
                TransactionContext.set(s);
                yield s;
            }
            case TransactionDefinition.PROPAGATION_NEVER -> {
                throw new IllegalStateException("Transaction exists but propagation is NEVER");
            }
            case TransactionDefinition.PROPAGATION_NESTED -> {
                // create savepoint on current resource (if supported)
                Object sp = createSavepoint(current.getResource());
                current.setSavepoint(sp);
                yield current;
            }
            default -> throw new IllegalArgumentException("Unknown propagation: " + def.getPropagation());
        };
    }

    private TransactionStatus startNew(TransactionDefinition def, Object suspended) {
        Object resource = doBegin(def);
        TransactionStatus.Simple status = new TransactionStatus.Simple(true, resource, suspended);
        TransactionContext.set(status);
        return status;
    }

    @Override
    public final void commit(TransactionStatus status) {
        // already completed?
        if (status.isCompleted()) {
            return;
        }

        // nested?
        if (status.getSavepoint() != null) {
            // nested commit is no-op
            TransactionContext.clear();
            return;
        }

        // normal tx
        if (status.isRollbackOnly()) {
            rollback(status);
            return;
        }

        // beforeCommit callbacks
        List<TransactionSynchronization> syncs = TransactionContext.synchronizations();
        for (var s : syncs) {
            s.beforeCommit(false);
        }
        for (var s : syncs) {
            s.beforeCompletion();
        }

        doCommit(status);

        if (status instanceof TransactionStatus.Simple s) {
            s.markCompleted();
        }
        TransactionContext.clear();

        // afterCommit callbacks
        for (var s : syncs) {
            s.afterCommit();
            s.afterCompletion(TransactionSynchronizations.STATUS_COMMITTED);
        }

        // resume suspended if any
        resumeIfNecessary(status);
    }

    @Override
    public final void rollback(TransactionStatus status) {
        // nested rollback => rollback to savepoint
        if (status.getSavepoint() != null) {
            rollbackToSavepoint(status.getResource(), status.getSavepoint());
            TransactionContext.clear();
            resumeIfNecessary(status);
            return;
        }

        doRollback(status);

        if (status instanceof TransactionStatus.Simple s) {
            s.markCompleted();
        }
        TransactionContext.clear();

        // synchronizations
        List<TransactionSynchronization> syncs = TransactionContext.synchronizations();
        for (var s : syncs) {
            s.afterCompletion(TransactionSynchronizations.STATUS_ROLLED_BACK);
        }

        // resume suspended if any
        resumeIfNecessary(status);
    }

    private void resumeIfNecessary(TransactionStatus status) {
        Object suspended = status.getSuspended();
        if (suspended != null) {
            resume(suspended);
        }
    }

    // --- hooks for concrete impls ---

    /**
     * Start underlying resource transaction.
     */
    protected abstract Object doBegin(TransactionDefinition def);

    protected abstract void doCommit(TransactionStatus status);

    protected abstract void doRollback(TransactionStatus status);

    /**
     * Suspend underlying resource(s) and return handle.
     */
    protected abstract Object suspend(TransactionStatus current);

    /**
     * Resume previously suspended resource(s).
     */
    protected abstract void resume(Object suspended);

    /**
     * Create savepoint on underlying resource if supported.
     */
    protected abstract Object createSavepoint(Object resource);

    /**
     * Roll back to previously created savepoint.
     */
    protected abstract void rollbackToSavepoint(Object resource, Object savepoint);
}
