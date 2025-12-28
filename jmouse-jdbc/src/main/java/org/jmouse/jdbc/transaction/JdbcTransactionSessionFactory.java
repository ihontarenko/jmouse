package org.jmouse.jdbc.transaction;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.TransactionSessionFactory;

public final class JdbcTransactionSessionFactory implements TransactionSessionFactory {

    private final ConnectionProvider connectionProvider;

    public JdbcTransactionSessionFactory(ConnectionProvider connectionProvider) {
        this.connectionProvider = Contract.nonNull(connectionProvider, "connectionProvider");
    }

    @Override
    public TransactionSession openSession(TransactionDefinition definition) {
        return new JdbcTransactionSession(connectionProvider);
    }
}
