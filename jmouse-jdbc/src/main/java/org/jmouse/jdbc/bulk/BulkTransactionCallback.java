package org.jmouse.jdbc.bulk;

@FunctionalInterface
public interface BulkTransactionCallback {

    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run() throws Exception;
    }

}
