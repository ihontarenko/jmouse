package org.jmouse.jdbc.transaction;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.TransactionSessionFactory;

public final class JdbcTransactionSessionFactory implements TransactionSessionFactory {

    private final ConnectionProvider   connectionProvider;
    private final ConnectionCustomizer customizer;

    public JdbcTransactionSessionFactory(
            ConnectionProvider connectionProvider, ConnectionCustomizer customizer
    ) {
        this.connectionProvider = Contract.nonNull(connectionProvider, "connectionProvider");
        this.customizer = Contract.nonNull(customizer, "customizer");
    }

    @Override
    public TransactionSession openSession(TransactionDefinition definition) {
        JdbcTransactionSession session = new JdbcTransactionSession(connectionProvider, customizer);
        session.configure(definition);
        return session;
    }
}
