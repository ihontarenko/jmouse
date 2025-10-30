package org.jmouse.tx;

/**
 * 🧠 Central transaction facade (like Spring's PlatformTransactionManager).
 */
public interface TransactionManager {

    TransactionStatus begin(TransactionDefinition definition);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);

    /**
     * 📦 Base for single-resource (JDBC/JPA) managers.
     */
    abstract class Support implements TransactionManager {

        @Override
        public final void commit(TransactionStatus status) {
            if (status.isRollbackOnly()) {
                rollback(status);
                return;
            }

            doCommit(status);

            if (status instanceof TransactionStatus.Simple implementation) {
                implementation.markCompleted();
            }

            TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_COMMITTED);
        }

        @Override
        public final void rollback(TransactionStatus status) {
            doRollback(status);

            if (status instanceof TransactionStatus.Simple implementation) {
                implementation.markCompleted();
            }

            TransactionSynchronizations.afterCompletion(TransactionSynchronizations.STATUS_ROLLED_BACK);
        }

        protected abstract void doCommit(TransactionStatus status);

        protected abstract void doRollback(TransactionStatus status);
    }
}
