package org.jmouse.tx.support;

import org.jmouse.tx.TransactionSession;

public class TransactionContextHolder {

    private static final ThreadLocal<TransactionSession> THREAD_LOCAL = new ThreadLocal<>();

    public static void bind(TransactionSession session) {
        THREAD_LOCAL.set(session);
    }

    public static TransactionSession get() {
        return THREAD_LOCAL.get();
    }

    public static void unbind() {
        THREAD_LOCAL.remove();
    }

}
