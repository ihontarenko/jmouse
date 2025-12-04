package org.jmouse.tx.support;

import org.jmouse.tx.*;

public abstract class AbstractTransactionManager implements FrameworkTransactionManager {

    @Override
    public final TransactionSession begin(TransactionDefinition definition) {
        TransactionDefinition transactionDefinition = (
                definition != null ? definition : TransactionDefinition.withDefaults()
        );

        Object  object   = doGetTransaction();
        boolean existing = isExisting(object);

        if (existing) {
            return handleExistingTransaction(transactionDefinition, object);
        }

        return handleNewTransaction(transactionDefinition, object);
    }

    @Override
    public final void commit(TransactionSession session) {
        if (session.isRollbackOnly()) {
            rollback(session);
            return;
        }

        TransactionSynchronizations.beforeCommit();
        TransactionSynchronizations.beforeCompletion();

        if (!session.isNew() && session.hasSavepoint()) {
            session.releaseSavepoint(session.getSavepoint());
        } else if (session.isNew()) {
            doCommit(session);
        }

        session.markCompleted();

        if (session.getSuspended() != null) {
            doResume(session.getResource(), session.getSuspended());
        }

        TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_COMMITTED);
        TransactionSynchronizationManager.clear();
    }

    @Override
    public final void rollback(TransactionSession status) {
        TransactionSynchronizations.beforeCompletion();

        if (!status.isNew() && status.hasSavepoint()) {
            rollbackSavepoint(status.getResource(), status.getSavepoint());
        } else if (status.isNew()) {
            doRollback(status);
        } else {
            status.markRollbackOnly();
        }

        status.markCompleted();

        if (status.getSuspended() != null) {
            doResume(status.getSavepoint(), status.getSuspended());
        }

        TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_ROLLED_BACK);
        TransactionSynchronizationManager.clear();
    }

    private TransactionSession handleNewTransaction(TransactionDefinition definition, Object object) {
        return switch (definition.getPropagation()) {
            case TransactionDefinition.PROPAGATION_REQUIRED,
                 TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                 TransactionDefinition.PROPAGATION_NESTED
                    -> newTransaction(definition, object, true, null);

            case TransactionDefinition.PROPAGATION_SUPPORTS,
                 TransactionDefinition.PROPAGATION_NEVER,
                 TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                    -> emptyTransaction(definition);

            case TransactionDefinition.PROPAGATION_MANDATORY ->
                    throw new IllegalStateException("No existing transaction for PROPAGATION_MANDATORY");

            default -> throw new IllegalArgumentException("Unsupported propagation: " + definition.getPropagation());
        };
    }

    private TransactionSession handleExistingTransaction(TransactionDefinition definition, Object object) {
        return switch (definition.getPropagation()) {
            case TransactionDefinition.PROPAGATION_REQUIRED,
                 TransactionDefinition.PROPAGATION_SUPPORTS,
                 TransactionDefinition.PROPAGATION_MANDATORY
                    -> joinTransaction(definition, object);

            case TransactionDefinition.PROPAGATION_REQUIRES_NEW
                    -> newTransaction(definition, object, true, doSuspend(object));

            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                    -> suspendedTransaction(definition, doSuspend(object));

            case TransactionDefinition.PROPAGATION_NEVER ->
                    throw new IllegalStateException("Existing transaction found for PROPAGATION_NEVER");

            case TransactionDefinition.PROPAGATION_NESTED -> {
                Object savepoint = null;

                try {
                    savepoint = createSavepoint(object);
                } catch (UnsupportedOperationException ignored) { }

                if (savepoint != null) {
                    TransactionSynchronizationManager.initialize(definition);
                    TransactionSession status = new TransactionSession.Simple(
                            definition.getName(), false, object, null);
                    status.setSavepoint(savepoint);
                    yield status;
                }

                yield joinTransaction(definition, object);
            }

            default -> throw new IllegalArgumentException(
                    "UNSUPPORTED PROPAGATION: " + definition.getPropagation());
        };
    }

    private TransactionSession newTransaction(
            TransactionDefinition definition,
            Object object,
            boolean newTransaction,
            Object suspended
    ) {
        TransactionSynchronizationManager.initialize(definition);

        doBegin(object, definition);

        return new TransactionSession.Simple(definition.getName(), newTransaction, object, suspended);
    }

    private TransactionSession joinTransaction(TransactionDefinition definition, Object object) {
        return newTransaction(definition, object, false, null);
    }

    private TransactionSession emptyTransaction(TransactionDefinition definition) {
        return newTransaction(definition, null, false, null);
    }

    private TransactionSession suspendedTransaction(TransactionDefinition definition, Object suspended) {
        return newTransaction(definition, null, false, suspended);
    }

    protected abstract Object doGetTransaction();

    protected abstract boolean isExisting(Object object);

    protected abstract void doBegin(Object object, TransactionDefinition definition);

    protected abstract void doCommit(TransactionSession status);

    protected abstract void doRollback(TransactionSession status);

    protected abstract Object doSuspend(Object current);

    protected abstract void doResume(Object current, Object suspended);

}
