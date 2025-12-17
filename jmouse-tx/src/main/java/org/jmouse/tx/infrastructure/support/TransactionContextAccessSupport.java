package org.jmouse.tx.infrastructure.support;

import org.jmouse.tx.infrastructure.TransactionContextHolder;

import java.util.Objects;

/**
 * Static access point for the active TransactionContextHolder.
 */
public final class TransactionContextAccessSupport {

    private static TransactionContextHolder HOLDER;

    private TransactionContextAccessSupport() {
    }

    public static void register(TransactionContextHolder contextHolder) {
        HOLDER = Objects.requireNonNull(contextHolder);
    }

    public static TransactionContextHolder current() {
        if (HOLDER == null) {
            throw new IllegalStateException(
                    "No transaction context holder registered."
            );
        }
        return HOLDER;
    }

    public static <T> T getResource(Class<T> key) {
        return current().getResource(key);
    }

    public static <T> void bindResource(Class<T> key, T resource) {
        current().bindResource(key, resource);
    }

    public static boolean hasResource(Class<?> key) {
        return current().hasResource(key);
    }

    public static <T> T unbindResource(Class<T> key) {
        return current().unbindResource(key);
    }
}
