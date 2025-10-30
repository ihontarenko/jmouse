package org.jmouse.tx;

public interface FrameworkTransactionManager {

    TransactionStatus begin(TransactionDefinition definition);

    void commit(TransactionStatus status);

    void rollback(TransactionStatus status);

}
