package org.jmouse.tx;

public interface TransactionCoordinator {

    TransactionSession begin(TransactionSpecification specification);

    void commit(TransactionSession session);

    void rollback(TransactionSession session);

}
