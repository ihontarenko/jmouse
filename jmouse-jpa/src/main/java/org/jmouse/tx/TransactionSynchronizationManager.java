package org.jmouse.tx;

import java.util.*;

public final class TransactionSynchronizationManager {

    private static final ThreadLocal<Context>             CONTEXT   = ThreadLocal.withInitial(Context::new);
    private static final ThreadLocal<Map<Object, Object>> RESOURCES = ThreadLocal.withInitial(HashMap::new);

    private TransactionSynchronizationManager() {
    }

    public static void bindResource(Object key, Object value) {
        Map<Object, Object> resources = RESOURCES.get();

        if (resources == null) {
            resources = new HashMap<>();
            RESOURCES.set(resources);
        }

        Object oldValue = resources.put(key, value);

        if (oldValue != null) {
            throw new IllegalStateException("Value [%s]:[%s] already bound to thread".formatted(key, oldValue));
        }
    }

    public static Object getResource(Object key) {
        Map<Object, Object> resources = RESOURCES.get();

        if (resources == null) {
            return null;
        }

        return resources.get(key);
    }

    public static boolean hasResource(Object key) {
        return getResource(key) != null;
    }

    public static void initialize(TransactionDefinition definition) {
        Context context = CONTEXT.get();
        context.synchronizations = new ArrayList<>();
        context.active = true;
        context.name = definition.getName();
        context.readOnly = definition.isReadOnly();
        context.isolation = definition.getIsolation();
    }

    public static boolean isSynchronizationActive() {
        return CONTEXT.get().active;
    }

    public static void registerSynchronization(TransactionSynchronization synchronization) {
        Context context = CONTEXT.get();

        if (!context.active) {
            throw new IllegalStateException("No active transaction synchronization for current thread");
        }

        context.synchronizations.add(synchronization);
    }

    public static List<TransactionSynchronization> getSynchronizations() {
        Context context = CONTEXT.get();

        if (context.synchronizations == null) {
            return Collections.emptyList();
        }

        return context.synchronizations;
    }

    public static String getCurrentTransactionName() {
        return CONTEXT.get().name;
    }

    public static boolean isCurrentTransactionReadOnly() {
        return CONTEXT.get().readOnly;
    }

    public static Integer getCurrentTransactionIsolationLevel() {
        int isolation = CONTEXT.get().isolation;

        if (isolation == TransactionDefinition.ISOLATION_DEFAULT) {
            return null;
        }

        return isolation;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static final class Context {
        List<TransactionSynchronization> synchronizations = new ArrayList<>();
        boolean                          active;
        String                           name;
        boolean                          readOnly;
        int                              isolation;
    }

}
