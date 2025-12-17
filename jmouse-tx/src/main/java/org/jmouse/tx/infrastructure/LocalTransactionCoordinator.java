package org.jmouse.tx.infrastructure;

import org.jmouse.tx.core.*;
import org.jmouse.tx.synchronization.*;

public class LocalTransactionCoordinator
        extends AbstractTransactionCoordinator {

    public LocalTransactionCoordinator(
            TransactionContextHolder contextHolder,
            TransactionSessionFactory sessionFactory,
            SynchronizationContextHolder synchronizationHolder
    ) {
        super(contextHolder, sessionFactory, synchronizationHolder);
    }

    @Override
    protected SynchronizationContextHolder getSynchronizationContextHolder() {
        return synchronizationHolder;
    }

    @Override
    protected TransactionSession doOpenSession(
            TransactionDefinition definition
    ) {
        return sessionFactory.openSession(definition);
    }

    @Override
    protected void doBegin(
            TransactionSession session,
            TransactionDefinition definition
    ) {
        session.begin();
    }

    @Override
    protected void doCommit(TransactionSession session) {
        session.commit();
    }

    @Override
    protected void doRollback(TransactionSession session) {
        session.rollback();
    }

    @Override
    protected void doSuspend(TransactionContext context) {
        // no-op for local transactions
        // hook for resource unbinding if needed
    }

    @Override
    protected void doResume(TransactionContext context) {
        // no-op for local transactions
    }

    @Override
    protected void doRollbackToSavepoint(
            TransactionSession session,
            Object savepoint
    ) {
        ((SavepointSupport) session).rollbackToSavepoint(savepoint);
    }

    @Override
    protected void doReleaseSavepoint(
            TransactionSession session,
            Object savepoint
    ) {
        ((SavepointSupport) session).releaseSavepoint(savepoint);
    }
}
