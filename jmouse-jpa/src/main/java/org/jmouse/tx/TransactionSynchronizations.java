package org.jmouse.tx;

import java.util.ArrayList;
import java.util.List;

public final class TransactionSynchronizations {

    public static final int STATUS_COMMITTED   = 0;
    public static final int STATUS_ROLLED_BACK = 1;
    public static final int STATUS_UNKNOWN     = 2;

    private static final ThreadLocal<List<TransactionSynchronization>> SYNCHRONIZATIONS =
            ThreadLocal.withInitial(ArrayList::new);

    private TransactionSynchronizations() {}

    public static void register(TransactionSynchronization synchronization) {
        SYNCHRONIZATIONS.get().add(synchronization);
    }

    public static void clear() {
        SYNCHRONIZATIONS.remove();
    }

    public static void beforeCommit(boolean readOnly) {
        for (TransactionSynchronization synchronization : SYNCHRONIZATIONS.get()) {
            synchronization.beforeCommit(readOnly);
        }
    }

    public static void beforeCompletion() {
        for (TransactionSynchronization synchronization : SYNCHRONIZATIONS.get()) {
            synchronization.beforeCompletion();
        }
    }

    public static void afterCommit() {
        for (TransactionSynchronization synchronization : SYNCHRONIZATIONS.get()) {
            synchronization.afterCommit();
        }
    }

    public static void afterCompletion(int status) {
        for (TransactionSynchronization synchronization : SYNCHRONIZATIONS.get()) {
            synchronization.afterCompletion(status);
        }
        clear();
    }
}