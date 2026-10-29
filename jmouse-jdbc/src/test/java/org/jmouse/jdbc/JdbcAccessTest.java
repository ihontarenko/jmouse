package org.jmouse.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jmouse.jdbc.connection.DataSourceConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.jdbc.transaction.JdbcResourceHolder;
import org.jmouse.transaction.DefaultTransactionDefinition;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JdbcAccessTest {
    @AfterEach
    void clear() {
        TransactionContextAccessSupport.current().clear();
    }

    @Test
    void plainConstructionNeedsNoSpringOrContainer() throws Exception {
        JdbcAccess access = JdbcAccess.from(source());
        assertEquals(42, access.jdbc().<Integer>queryOne("select 42", (row, index) -> row.getInt(1)));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.context.ApplicationContext"));
    }

    @Test
    void namedParametersRepeatAndIgnoreQuotedColons() throws Exception {
        JdbcAccess access = JdbcAccess.from(source());
        String result = access.named().querySingle(
                "select ':literal' as marker, :id as a, :id as b", Map.of("id", 7),
                (row, index) -> row.getString("marker") + ":" + row.getInt("a") + ":" + row.getInt("b")).orElseThrow();
        assertEquals(":literal:7:7", result);
    }

    @Test
    void sameDataSourceClientsShareTheTransaction() throws Exception {
        JdbcDataSource source = source();
        JdbcAccess first = JdbcAccess.from(source);
        JdbcAccess second = JdbcAccess.from(source);
        first.jdbc().update("create table entries (id integer)");
        assertThrows(IllegalStateException.class, () -> first.transactions().inTransaction(
                DefaultTransactionDefinition.builder().build(), () -> {
                    second.jdbc().update("insert into entries values (1)");
                    throw new IllegalStateException("rollback both clients");
                }));
        assertEquals(0, first.jdbc().<Integer>queryOne("select count(*) from entries", (row, index) -> row.getInt(1)));
    }

    @Test
    void anotherDataSourceNeverBorrowsTheActiveDatasourcesConnection() throws Exception {
        JdbcDataSource firstSource = source();
        JdbcDataSource secondSource = source();
        JdbcAccess first = JdbcAccess.from(firstSource);
        JdbcAccess second = JdbcAccess.from(secondSource);
        first.jdbc().update("create table entries (id integer)");
        second.jdbc().update("create table entries (id integer)");
        second.jdbc().update("insert into entries values (7)");
        first.transactions().inTransaction(DefaultTransactionDefinition.builder().build(), () -> {
            first.jdbc().update("insert into entries values (1)");
            assertEquals(7, second.jdbc().<Integer>queryOne("select id from entries", (row, index) -> row.getInt(1)));
            assertEquals(1, first.jdbc().<Integer>queryOne("select id from entries", (row, index) -> row.getInt(1)));
            return null;
        });
    }

    @Test
    void releasingAnUnboundConnectionDoesNotDependOnAnyHolderBeingPresent() throws Exception {
        JdbcDataSource first = source();
        JdbcDataSource second = source();
        try (Connection owned = first.getConnection()) {
            TransactionContextAccessSupport.bindResource(JdbcResourceHolder.class, new JdbcResourceHolder(owned, first));
            TransactionAwareConnectionProvider provider = new TransactionAwareConnectionProvider(
                    new DataSourceConnectionProvider(second));
            Connection foreign = provider.getConnection();
            assertNotSame(owned, foreign);
            provider.release(foreign);
            assertTrue(foreign.isClosed());
            assertFalse(owned.isClosed());
        }
    }

    @Test
    void releasingTheActualTransactionConnectionKeepsItOpen() throws Exception {
        JdbcDataSource source = source();
        try (Connection owned = source.getConnection()) {
            TransactionContextAccessSupport.bindResource(JdbcResourceHolder.class, new JdbcResourceHolder(owned, source));
            TransactionAwareConnectionProvider provider = new TransactionAwareConnectionProvider(
                    new DataSourceConnectionProvider(source));
            assertSame(owned, provider.getConnection());
            provider.release(owned);
            assertFalse(owned.isClosed());
        }
    }

    private JdbcDataSource source() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return source;
    }
}
