package org.jmouse.tx;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧵 Thread-local transaction context (current status + synchronizations).
 */
public final class TransactionContext {

    private static final ThreadLocal<TransactionStatus> CURRENT =
            new ThreadLocal<>();

    private static final ThreadLocal<List<TransactionSynchronization>> SYNC =
            ThreadLocal.withInitial(ArrayList::new);

    private TransactionContext() {}

    public static TransactionStatus current() {
        return CURRENT.get();
    }

    public static void set(TransactionStatus status) {
        CURRENT.set(status);
    }

    public static void clear() {
        CURRENT.remove();
        SYNC.remove();
    }

    public static void register(TransactionSynchronization s) {
        SYNC.get().add(s);
    }

    public static List<TransactionSynchronization> synchronizations() {
        return SYNC.get();
    }
}
