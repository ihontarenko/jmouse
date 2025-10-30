package org.jmouse.tx;

public final class TransactionSynchronizations {

    public static final int STATUS_COMMITTED   = TransactionSynchronization.STATUS_COMMITTED;
    public static final int STATUS_ROLLED_BACK = TransactionSynchronization.STATUS_ROLLED_BACK;
    public static final int STATUS_UNKNOWN     = TransactionSynchronization.STATUS_UNKNOWN;

    private TransactionSynchronizations() {
    }

    public static void beforeCommit() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.beforeCommit(
                    TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            );
        }
    }

    public static void beforeCompletion() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.beforeCompletion();
        }
    }

    public static void afterCompletion(int status) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(status);
        }
    }
}
