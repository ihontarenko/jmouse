package org.jmouse.tx;

public abstract class AbstractTransactionManager implements FrameworkTransactionManager {

    @Override
    public final TransactionStatus begin(TransactionDefinition definition) {
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
    public final void commit(TransactionStatus status) {
        if (status.isRollbackOnly()) {
            rollback(status);
            return;
        }

        TransactionSynchronizations.beforeCommit();
        TransactionSynchronizations.beforeCompletion();

        if (!status.isNew() && status.hasSavepoint()) {
            releaseSavepoint(status.getResource(), status.getSavepoint());
        } else if (status.isNew()) {
            doCommit(status);
        }

        status.markCompleted();

        if (status.getSuspendedResources() != null) {
            doResume(status.getResource(), status.getSuspendedResources());
        }

        TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_COMMITTED);
        TransactionSynchronizationManager.clear();
    }

    @Override
    public final void rollback(TransactionStatus status) {
        TransactionSynchronizations.beforeCompletion();

        if (!status.isNew() && status.hasSavepoint()) {
            rollbackSavepoint(status.getResource(), status.getSavepoint());
        } else if (status.isNew()) {
            doRollback(status);
        } else {
            status.setRollbackOnly();
        }

        status.markCompleted();

        if (status.getSuspendedResources() != null) {
            doResume(status.getResource(), status.getSuspendedResources());
        }

        TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_ROLLED_BACK);
        TransactionSynchronizationManager.clear();
    }

    private TransactionStatus handleNewTransaction(TransactionDefinition definition, Object object) {
        return switch (definition.getPropagation()) {
            case TransactionDefinition.PROPAGATION_REQUIRED,
                 TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                 TransactionDefinition.PROPAGATION_NESTED
                    -> newTransaction(definition, object, true, null);

            case TransactionDefinition.PROPAGATION_SUPPORTS,
                 TransactionDefinition.PROPAGATION_NEVER,
                 TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> emptyTransaction(definition);

            case TransactionDefinition.PROPAGATION_MANDATORY ->
                    throw new IllegalStateException("No existing transaction for PROPAGATION_MANDATORY");

            default -> throw new IllegalArgumentException("Unsupported propagation: " + definition.getPropagation());
        };
    }

    private TransactionStatus handleExistingTransaction(TransactionDefinition definition, Object object) {
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
                    TransactionStatus status = new TransactionStatus.Simple(
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

    private TransactionStatus newTransaction(
            TransactionDefinition definition,
            Object object,
            boolean newTransaction,
            Object suspended
    ) {
        TransactionSynchronizationManager.initialize(definition);

        doBegin(object, definition);

        return new TransactionStatus.Simple(definition.getName(), newTransaction, object, suspended);
    }

    private TransactionStatus joinTransaction(TransactionDefinition definition, Object object) {
        return newTransaction(definition, object, false, null);
    }

    private TransactionStatus emptyTransaction(TransactionDefinition definition) {
        return newTransaction(definition, null, false, null);
    }

    private TransactionStatus suspendedTransaction(TransactionDefinition definition, Object suspended) {
        return newTransaction(definition, null, false, suspended);
    }

    protected abstract Object doGetTransaction();

    protected abstract boolean isExisting(Object object);

    protected abstract void doBegin(Object object, TransactionDefinition definition);

    protected abstract void doCommit(TransactionStatus status);

    protected abstract void doRollback(TransactionStatus status);

    protected Object doSuspend(Object current) {
        throw new UnsupportedOperationException("SUSPEND NOT SUPPORTED BY: " + getClass().getName());
    }

    protected void doResume(Object current, Object suspended) {
        throw new UnsupportedOperationException("RESUME NOT SUPPORTED BY: " + getClass().getName());
    }

    protected Object createSavepoint(Object resource) {
        throw new UnsupportedOperationException("SAVE-POINTS NOT SUPPORTED BY: " + getClass().getName());
    }

    protected void rollbackSavepoint(Object resource, Object savepoint) {
        throw new UnsupportedOperationException("SAVE-POINTS NOT SUPPORTED BY: " + getClass().getName());
    }

    protected void releaseSavepoint(Object resource, Object savepoint) {

    }

}
