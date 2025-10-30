package org.jmouse.jta;

import jakarta.transaction.*;
import org.jmouse.tx.AbstractTransactionManager;
import org.jmouse.tx.TransactionDefinition;
import org.jmouse.tx.TransactionStatus;

public class JtaTransactionManager extends AbstractTransactionManager {

    private final TransactionManager                 transactionManager;
    private final UserTransaction                    userTransaction;
    private final TransactionSynchronizationRegistry synchronizationRegistry;

    public JtaTransactionManager(UserTransaction transaction) {
        this(transaction, null, null);
    }

    public JtaTransactionManager(UserTransaction transaction, TransactionManager manager) {
        this(transaction, manager, null);
    }

    public JtaTransactionManager(
            UserTransaction transaction,
            TransactionManager manager,
            TransactionSynchronizationRegistry registry
    ) {
        this.userTransaction = transaction;
        this.transactionManager = manager;
        this.synchronizationRegistry = registry;
    }

    @Override
    protected Object doGetTransaction() {
        return new JtaTxObject(userTransaction);
    }

    @Override
    protected boolean isExisting(Object object) {
        if (object instanceof JtaTxObject(UserTransaction transaction)) {
            try {
                return transaction.getStatus() != Status.STATUS_NO_TRANSACTION;
            } catch (SystemException e) {
                throw new IllegalStateException("JTA getStatus failed", e);
            }
        }
        return false;
    }

    @Override
    protected void doBegin(Object txObject, TransactionDefinition definition) {
        JtaTxObject jta = (JtaTxObject) txObject;
        try {
            if (definition.getTimeout() > 0) {
                jta.transaction.setTransactionTimeout(definition.getTimeout());
            }
            jta.transaction.begin();
        } catch (NotSupportedException | SystemException e) {
            throw new IllegalStateException("Cannot start JTA transaction", e);
        }
    }

    @Override
    protected void doCommit(TransactionStatus status) {
        try {
            userTransaction.commit();
        } catch (RollbackException e) {
            throw new IllegalStateException("JTA rolled back", e);
        } catch (HeuristicMixedException | HeuristicRollbackException | SystemException e) {
            throw new IllegalStateException("JTA commit failed", e);
        }
    }

    @Override
    protected void doRollback(TransactionStatus status) {
        try {
            userTransaction.rollback();
        } catch (SystemException e) {
            throw new IllegalStateException("JTA rollback failed", e);
        }
    }

    @Override
    protected Object doSuspend(Object txObject) {
        if (transactionManager == null) {
            throw new UnsupportedOperationException("No JTA TransactionManager – suspend not supported");
        }
        try {
            return transactionManager.suspend();
        } catch (SystemException e) {
            throw new IllegalStateException("JTA suspend failed", e);
        }
    }

    @Override
    protected void doResume(Object txObject, Object suspended) {
        if (transactionManager == null) {
            return;
        }
        try {
            transactionManager.resume((Transaction) suspended);
        } catch (InvalidTransactionException | SystemException e) {
            throw new IllegalStateException("JTA resume failed", e);
        }
    }

    private record JtaTxObject(UserTransaction transaction) {}

}
