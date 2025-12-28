package org.jmouse.transaction;

@FunctionalInterface
public interface TransactionCallback {
    <T> T inTransaction(TransactionDefinition definition, Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run() throws Exception;
    }
}
