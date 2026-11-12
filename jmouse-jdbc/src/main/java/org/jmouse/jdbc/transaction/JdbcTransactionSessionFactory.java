package org.jmouse.jdbc.transaction;

import java.util.Objects;
import org.jmouse.core.Verify;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionSession;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.TransactionSessionFactory;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

public final class JdbcTransactionSessionFactory implements TransactionSessionFactory {

    private final ConnectionProvider connectionProvider;
    private final ConnectionCustomizer customizer;
    private final TransactionContextHolder contexts;

    public JdbcTransactionSessionFactory(
        ConnectionProvider connectionProvider,
        ConnectionCustomizer customizer
    ) {
        this(connectionProvider, customizer, TransactionContextAccessSupport.current());
    }

    public JdbcTransactionSessionFactory(
        ConnectionProvider connectionProvider,
        ConnectionCustomizer customizer,
        TransactionContextHolder contexts
    ) {
        this.connectionProvider = Verify.nonNull(connectionProvider, "connectionProvider");
        this.customizer = Verify.nonNull(customizer, "customizer");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    @Override
    public boolean supportsExistingSession(TransactionSession session) {
        return (
            session instanceof JdbcTransactionSession jdbcSession &&
            jdbcSession.belongsTo(connectionProvider.getResourceKey())
        );
    }

    @Override
    public TransactionSession openSession(TransactionDefinition definition) {
        JdbcTransactionSession session = new JdbcTransactionSession(connectionProvider, customizer, contexts);
        session.configure(definition);
        return session;
    }
}
