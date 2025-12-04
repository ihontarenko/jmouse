package org.jmouse.tx.support;

import org.jmouse.tx.*;

public abstract class AbstractTransactionCoordinator implements TransactionCoordinator, SavepointManager {

    @Override
    public final TransactionSession begin(TransactionSpecification specification) {
        TransactionSession session = doBegin(specification);
        SynchronizationsKeeper.init();
        return session;
    }

    @Override
    public final void commit(TransactionSession session) {
//        if (session.isRollbackOnly()) {
//            rollback(session);
//            return;
//        }

        try {
            doCommit(session);
            SynchronizationsKeeper.beforeCompletion();
        } catch (Exception e) {
            rollback(session);
            throw e;
        } finally {
            SynchronizationsKeeper.iterate(TransactionSynchronization::afterCompletion);
            SynchronizationsKeeper.clear();
        }
    }

    @Override
    public final void rollback(TransactionSession session) {
        try {
            doRollback(session);
        } finally {
            SynchronizationsKeeper.afterCompletion();
            SynchronizationsKeeper.clear();
        }
    }

    @Override
    public final Object createSavepoint(TransactionSession session) {
        return getSavepointManager(session).createSavepoint();
    }

    @Override
    public final void rollbackToSavepoint(TransactionSession session, Object savepoint) {
        // getSavepointManager(session).rollbackToSavepoint(savepoint);
    }

    private void doResolveNew() {

    }

    private void doResolveExisting() {

    }

    protected abstract TransactionSession doBegin(TransactionSpecification spec);

    protected abstract void doCommit(TransactionSession session);

    protected abstract void doRollback(TransactionSession session);

    protected abstract SavepointManager getSavepointManager(TransactionSession session);
}
