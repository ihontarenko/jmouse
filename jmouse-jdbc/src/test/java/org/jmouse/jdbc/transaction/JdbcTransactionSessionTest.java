package org.jmouse.jdbc.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.exception.JdbcAccessException;
import org.jmouse.transaction.DefaultTransactionDefinition;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.jmouse.transaction.infrastructure.thread.ThreadBoundTransactionContextHolder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTransactionSessionTest {
    private final ThreadBoundTransactionContextHolder holder = new ThreadBoundTransactionContextHolder();
    private String url;

    @BeforeEach
    void prepare() throws SQLException {
        holder.clear();
        TransactionContextAccessSupport.register(holder);
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("create table entries (id integer primary key)");
        }
    }

    @AfterEach
    void clear() {
        holder.clear();
    }

    @Test
    void closingUnfinishedWorkRollsBackInsteadOfImplicitlyCommitting() throws Exception {
        Probe provider = new Probe();
        JdbcTransactionSession session = session(provider, new IsolationReadOnlyConnectionCustomizer());
        session.begin();
        insert(provider.connection);
        session.close();
        assertEquals(0, count());
        assertEquals(1, provider.releases);
        assertFalse(session.isActive());
        assertFalse(holder.hasResource(JdbcResourceHolder.class));
    }

    @Test
    void completedCommitSurvivesClose() throws Exception {
        Probe provider = new Probe();
        JdbcTransactionSession session = session(provider, new IsolationReadOnlyConnectionCustomizer());
        session.begin();
        insert(provider.connection);
        session.commit();
        session.close();
        assertEquals(1, count());
        assertTrue(provider.autoCommitAtRelease);
    }

    @Test
    void restoresAnInitiallyDisabledAutoCommit() throws Exception {
        Probe provider = new Probe();
        provider.connection.setAutoCommit(false);
        JdbcTransactionSession session = session(provider, new IsolationReadOnlyConnectionCustomizer());
        session.begin();
        session.commit();
        session.close();
        assertFalse(provider.autoCommitAtRelease);
    }

    @Test
    void failedBeginRestoresAndReleasesAnAcquiredConnection() throws Exception {
        Probe provider = new Probe();
        int isolation = provider.connection.getTransactionIsolation();
        JdbcTransactionSession session = session(provider, (connection, definition) -> {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            throw new SQLException("setup failure");
        });
        JdbcAccessException failure = assertThrows(JdbcAccessException.class, session::begin);
        assertEquals("setup failure", failure.getCause().getMessage());
        assertEquals(1, provider.releases);
        assertEquals(isolation, provider.isolationAtRelease);
        assertFalse(session.isActive());
        assertFalse(holder.hasResource(JdbcResourceHolder.class));
    }

    @Test
    void failedBindingDoesNotRemoveAnotherSessionsResource() throws Exception {
        try (Connection existing = DriverManager.getConnection(url)) {
            JdbcResourceHolder original = new JdbcResourceHolder(existing);
            holder.bindResource(JdbcResourceHolder.class, original);
            Probe provider = new Probe();
            JdbcTransactionSession session = session(provider, new IsolationReadOnlyConnectionCustomizer());
            assertThrows(IllegalStateException.class, session::begin);
            assertSame(original, holder.getResource(JdbcResourceHolder.class));
            assertEquals(1, provider.releases);
            assertFalse(session.isActive());
        }
    }

    @Test
    void closingAnUnusedSessionDoesNotRemoveAnotherSessionsResource() throws Exception {
        Probe provider = new Probe();
        JdbcResourceHolder original = new JdbcResourceHolder(provider.connection);
        holder.bindResource(JdbcResourceHolder.class, original);
        session(provider, new IsolationReadOnlyConnectionCustomizer()).close();
        assertSame(original, holder.getResource(JdbcResourceHolder.class));
        assertEquals(0, provider.releases);
        provider.connection.close();
    }

    @Test
    void repeatedBeginDoesNotAcquireOrLeakAnotherConnection() throws Exception {
        Probe provider = new Probe();
        JdbcTransactionSession session = session(provider, new IsolationReadOnlyConnectionCustomizer());
        session.begin();
        assertThrows(IllegalStateException.class, session::begin);
        assertEquals(1, provider.acquisitions);
        session.rollback();
        session.close();
    }

    @Test
    void restorationFailureStillUnbindsAndReleases() throws Exception {
        Probe provider = new Probe();
        JdbcTransactionSession session = session(provider, (connection, definition) -> () -> {
            throw new SQLException("restore failure");
        });
        session.begin();
        session.commit();
        assertThrows(JdbcAccessException.class, session::close);
        assertEquals(1, provider.releases);
        assertFalse(holder.hasResource(JdbcResourceHolder.class));
        assertFalse(session.isActive());
    }

    private JdbcTransactionSession session(Probe provider, ConnectionCustomizer customizer) {
        JdbcTransactionSession session = new JdbcTransactionSession(provider, customizer);
        session.configure(DefaultTransactionDefinition.builder().build());
        return session;
    }

    private void insert(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("insert into entries values (1)");
        }
    }

    private int count() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from entries")) {
            result.next();
            return result.getInt(1);
        }
    }

    private final class Probe implements ConnectionProvider {
        final Connection connection = DriverManager.getConnection(url);
        int acquisitions;
        int releases;
        boolean autoCommitAtRelease;
        int isolationAtRelease;

        Probe() throws SQLException { }

        @Override public Connection getConnection() { acquisitions++; return connection; }

        @Override public void release(Connection released) {
            releases++;
            try {
                autoCommitAtRelease = released.getAutoCommit();
                isolationAtRelease = released.getTransactionIsolation();
                released.close();
            } catch (SQLException failure) {
                throw new IllegalStateException(failure);
            }
        }
    }
}
