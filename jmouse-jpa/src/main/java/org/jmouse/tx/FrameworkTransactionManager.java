package org.jmouse.tx;

public interface FrameworkTransactionManager {

    TransactionSession begin(TransactionDefinition definition);

    void commit(TransactionSession status);

    void rollback(TransactionSession status);

}
