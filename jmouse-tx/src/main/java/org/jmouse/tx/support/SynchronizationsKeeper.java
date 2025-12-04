package org.jmouse.tx.support;

import org.jmouse.tx.TransactionSynchronization;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class SynchronizationsKeeper {

    private static final ThreadLocal<Set<TransactionSynchronization>> SYNCHRONIZATIONS =
            ThreadLocal.withInitial(HashSet::new);

    public static void register(TransactionSynchronization synchronization) {
        SYNCHRONIZATIONS.get().add(synchronization);
    }

    public static void iterate(Consumer<TransactionSynchronization> synchronizationConsumer) {
        SYNCHRONIZATIONS.get().forEach(synchronizationConsumer);
    }

    public static void beforeCompletion() {
        SYNCHRONIZATIONS.get().forEach(TransactionSynchronization::beforeCompletion);
    }

    public static void afterCompletion() {
        SYNCHRONIZATIONS.get().forEach(TransactionSynchronization::afterCompletion);
    }

    public static void init() {
        SYNCHRONIZATIONS.get().clear();
    }

    public static void clear() {
        SYNCHRONIZATIONS.remove();
    }
}