package org.jmouse.tx;

public interface TransactionSession {

    TransactionSession begin(TransactionSpecification specification);

    void commit(TransactionSession session);

    void rollback(TransactionSession session);

    void rollbackTo(TransactionSession session, Object savepoint);

    Object createSavepoint(TransactionSession session);

}
