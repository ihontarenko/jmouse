package org.jmouse.jdbc.transaction;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.tx.core.TransactionDefinition;
import org.jmouse.tx.core.TransactionSession;
import org.jmouse.tx.infrastructure.TransactionSessionFactory;

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
