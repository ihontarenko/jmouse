package org.jmouse.jdbc.transaction;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jmouse.jdbc.JdbcTemplate;
import org.jmouse.jdbc.SQLExecutor;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.connection.DataSourceConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.transaction.*;
import org.jmouse.transaction.infrastructure.*;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.jmouse.transaction.infrastructure.thread.ThreadBoundTransactionContextHolder;
import org.jmouse.transaction.synchronization.ThreadBoundSynchronizationContextHolder;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class JdbcPropagationTest {
    private final ThreadBoundTransactionContextHolder contexts = new ThreadBoundTransactionContextHolder();
    private final ThreadBoundSynchronizationContextHolder synchronizations = new ThreadBoundSynchronizationContextHolder();
    private ConnectionProvider raw;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;

    @BeforeEach
    void prepare() throws Exception {
        contexts.clear();
        TransactionContextAccessSupport.register(contexts);
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        raw = new DataSourceConnectionProvider(source);
        jdbc = new JdbcTemplate.Default(new SQLExecutor(new TransactionAwareConnectionProvider(raw)));
        transactions = template(new IsolationReadOnlyConnectionCustomizer());
        jdbc.update("create table entries (id integer primary key)");
    }

    @AfterEach
    void clear() {
        contexts.clear();
    }

    @Test
    void requiredJoinsWithoutPrematureCommit() {
        RuntimeException original = new IllegalStateException("outer rollback");
        assertSame(original, assertThrows(RuntimeException.class, () -> transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            jdbc.update("insert into entries values (1)");
            transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
                jdbc.update("insert into entries values (2)");
                return null;
            });
            assertNotNull(contexts.getContext());
            throw original;
        })));
        assertEquals(0, count());
        assertNull(contexts.getContext());
    }

    @Test
    void caughtFailureInRequiredMarksTheOuterTransactionRollbackOnly() {
        transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            jdbc.update("insert into entries values (1)");
            assertThrows(IllegalStateException.class, () -> transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
                throw new IllegalStateException("joined failure");
            }));
            assertTrue(contexts.getContext().isRollbackOnly());
            jdbc.update("insert into entries values (2)");
            return null;
        });
        assertEquals(0, count());
        assertNull(contexts.getContext());
    }

    @Test
    void requiresNewCommitSurvivesOuterRollback() {
        assertThrows(IllegalStateException.class, () -> transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            jdbc.update("insert into entries values (1)");
            transactions.inTransaction(definition(TransactionPropagation.REQUIRES_NEW), () -> {
                jdbc.update("insert into entries values (2)");
                return null;
            });
            throw new IllegalStateException("outer failure");
        }));
        assertEquals(1, count());
        assertNull(contexts.getContext());
    }

    @Test
    void requiresNewRollbackDoesNotCommitItsWrites() {
        transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            jdbc.update("insert into entries values (1)");
            assertThrows(IllegalStateException.class, () -> transactions.inTransaction(definition(TransactionPropagation.REQUIRES_NEW), () -> {
                jdbc.update("insert into entries values (2)");
                throw new IllegalStateException("inner failure");
            }));
            return null;
        });
        assertEquals(1, count());
    }

    @Test
    void nestedFailureRollsBackOnlyToSavepoint() {
        transactions.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            jdbc.update("insert into entries values (1)");
            assertThrows(IllegalStateException.class, () -> transactions.inTransaction(definition(TransactionPropagation.NESTED), () -> {
                jdbc.update("insert into entries values (2)");
                throw new IllegalStateException("savepoint rollback");
            }));
            jdbc.update("insert into entries values (3)");
            return null;
        });
        assertEquals(2, count());
        assertNull(contexts.getContext());
    }

    @Test
    void failingRequiresNewStartupRestoresTheOuterContext() {
        AtomicInteger calls = new AtomicInteger();
        TransactionTemplate special = template((connection, definition) -> {
            if (calls.incrementAndGet() == 2) {
                throw new SQLException("inner begin failure");
            }
            return new IsolationReadOnlyConnectionCustomizer().apply(connection, definition);
        });
        special.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            TransactionContext original = contexts.getContext();
            assertThrows(RuntimeException.class, () -> special.inTransaction(definition(TransactionPropagation.REQUIRES_NEW), () -> null));
            assertSame(original, contexts.getContext());
            jdbc.update("insert into entries values (1)");
            return null;
        });
        assertEquals(1, count());
    }

    @Test
    void failingInnerCleanupStillRestoresTheOuterContext() {
        AtomicInteger calls = new AtomicInteger();
        TransactionTemplate special = template((connection, definition) -> {
            if (calls.incrementAndGet() == 2) {
                return () -> { throw new SQLException("inner close failure"); };
            }
            return new IsolationReadOnlyConnectionCustomizer().apply(connection, definition);
        });
        special.inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            TransactionContext original = contexts.getContext();
            assertThrows(RuntimeException.class, () -> special.inTransaction(definition(TransactionPropagation.REQUIRES_NEW), () -> null));
            assertSame(original, contexts.getContext());
            jdbc.update("insert into entries values (1)");
            return null;
        });
        assertEquals(1, count());
    }

    private TransactionTemplate template(ConnectionCustomizer customizer) {
        JoinTransactionValidator validator = new JoinTransactionValidator(
                JoinAttributePolicy.FAIL, JoinAttributePolicy.FAIL, JoinAttributePolicy.FAIL,
                (message, failure) -> { });
        return new TransactionTemplate(new JdbcTransactionCoordinator(contexts,
                new JdbcTransactionSessionFactory(raw, customizer), synchronizations, validator));
    }

    private TransactionDefinition definition(TransactionPropagation propagation) {
        return DefaultTransactionDefinition.builder().propagation(propagation).build();
    }

    private int count() {
        try {
            return jdbc.queryOne("select count(*) from entries", (row, index) -> row.getInt(1));
        } catch (SQLException failure) {
            throw new AssertionError(failure);
        }
    }
}
