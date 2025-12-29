package org.jmouse.jdbc.transaction;

import org.jmouse.transaction.SavepointSupport;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.*;
import org.jmouse.transaction.synchronization.SynchronizationContextHolder;

import java.util.Map;

public final class JdbcTransactionCoordinator extends AbstractTransactionCoordinator {

    public JdbcTransactionCoordinator(
            TransactionContextHolder contextHolder,
            TransactionSessionFactory transactionSessionFactory,
            SynchronizationContextHolder synchronizationHolder,
            JoinTransactionValidator joinValidator
    ) {
        super(contextHolder, transactionSessionFactory, synchronizationHolder, joinValidator);
    }

    @Override
    protected SynchronizationContextHolder getSynchronizationContextHolder() {
        return synchronizationHolder;
    }

    @Override
    protected void doBegin(TransactionSession session, TransactionDefinition definition) {
        // later: apply isolation/readOnly/timeout at session-level if you want
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
    protected void doSuspend(TransactionContext context, Map<Class<?>, Object> snapshot) {
        // no-op: snapshot already detached resources from thread
        // optional: metrics/logging
    }

    @Override
    protected void doResume(TransactionContext context, Map<Class<?>, Object> snapshot) {
        // no-op: snapshot already restored resources to thread
    }

    @Override
    protected void doRollbackToSavepoint(TransactionSession session, Object savepoint) {
        if (session instanceof SavepointSupport savepointSupport) {
            savepointSupport.rollbackToSavepoint(savepoint);
        } else {
            throw new IllegalStateException("Session does not support save-points");
        }
    }

    @Override
    protected void doReleaseSavepoint(TransactionSession session, Object savepoint) {
        if (session instanceof SavepointSupport savepointSupport) {
            savepointSupport.releaseSavepoint(savepoint);
        } else {
            throw new IllegalStateException("Session does not support save-points");
        }
    }
}
