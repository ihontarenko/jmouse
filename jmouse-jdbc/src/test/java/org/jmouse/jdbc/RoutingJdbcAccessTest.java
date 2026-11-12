package org.jmouse.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.jdbc.connection.RoutingConnectionProvider;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceRegistry;
import org.jmouse.transaction.DefaultTransactionDefinition;
import org.jmouse.transaction.TransactionPropagation;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RoutingJdbcAccessTest {

    @AfterEach
    void clear() {
        DataSourceKeyHolder.clear();
        TransactionContextAccessSupport.current().clear();
    }

    @Test
    void routingSelectsOnceAndPinsConnectionForTheTransaction() throws Exception {
        JdbcDataSource first = source();
        JdbcDataSource second = source();
        JdbcAccess firstAccess = prepared(first);
        JdbcAccess secondAccess = prepared(second);
        DataSourceRegistry registry = new DataSourceRegistry();
        registry.register("first", first);
        registry.register("second", second);
        AtomicReference<String> key = new AtomicReference<>("first");
        AtomicInteger resolutions = new AtomicInteger();
        JdbcAccess routed = JdbcAccess.using(
            new RoutingConnectionProvider(name -> {
                resolutions.incrementAndGet();
                return registry.resolve(name);
            }, key::get)
        );
        assertThrows(IllegalStateException.class, () ->
            routed.transactions().inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
                routed.jdbc().update("insert into entries values (1)");
                key.set("second");
                routed.jdbc().update("insert into entries values (2)");
                assertEquals(2, count(routed));
                routed.transactions().inTransaction(definition(TransactionPropagation.REQUIRES_NEW), () -> {
                    routed.jdbc().update("insert into entries values (3)");
                    return null;
                });
                assertEquals(2, count(routed));
                throw new IllegalStateException("rollback first");
            })
        );
        assertEquals(2, resolutions.get());
        assertEquals(0, count(firstAccess));
        assertEquals(1, count(secondAccess));
        assertNull(TransactionContextAccessSupport.current().getContext());
    }

    @Test
    void differentDatasourceManagersHaveIndependentLocalTransactions() throws Exception {
        JdbcAccess first = prepared(source());
        JdbcAccess second = prepared(source());
        assertThrows(IllegalStateException.class, () ->
            first.transactions().inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
                first.jdbc().update("insert into entries values (1)");
                second.transactions().inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
                    second.jdbc().update("insert into entries values (2)");
                    return null;
                });
                assertEquals(1, count(first));
                throw new IllegalStateException("rollback first only");
            })
        );
        assertEquals(0, count(first));
        assertEquals(1, count(second));
        assertNull(TransactionContextAccessSupport.current().getContext());
    }

    @Test
    void foreignTransactionDoesNotSatisfyMandatoryAndOuterRemainsUsable() throws Exception {
        JdbcAccess first = prepared(source());
        JdbcAccess second = prepared(source());
        first.transactions().inTransaction(definition(TransactionPropagation.REQUIRED), () -> {
            assertThrows(IllegalStateException.class, () ->
                second
                    .transactions()
                    .inTransaction(definition(TransactionPropagation.MANDATORY), () ->
                        fail("must not execute")
                    )
            );
            first.jdbc().update("insert into entries values (1)");
            return null;
        });
        assertEquals(1, count(first));
    }

    @Test
    void scopedKeysRestoreNestedStateAndFailures() {
        DataSourceKeyHolder.use("initial");
        assertThrows(IllegalStateException.class, () -> {
            try (var first = DataSourceKeyHolder.withKey("first")) {
                try (var second = DataSourceKeyHolder.withKey("second")) {
                    assertEquals("second", DataSourceKeyHolder.current());
                }
                assertEquals("first", DataSourceKeyHolder.current());
                throw new IllegalStateException("work failed");
            }
        });
        assertEquals("initial", DataSourceKeyHolder.current());
    }

    @Test
    void scopesRejectOutOfOrderCloseWithoutLosingState() {
        var first = DataSourceKeyHolder.withKey("first");
        var second = DataSourceKeyHolder.withKey("second");
        try {
            assertThrows(IllegalStateException.class, first::close);
            assertEquals("second", DataSourceKeyHolder.current());
        } finally {
            second.close();
            first.close();
        }
        assertNull(DataSourceKeyHolder.current());
    }

    private JdbcAccess prepared(JdbcDataSource source) throws Exception {
        JdbcAccess access = JdbcAccess.from(source);
        access.jdbc().update("create table entries (id integer)");
        return access;
    }

    private int count(JdbcAccess access) throws Exception {
        return access.jdbc().queryOne("select count(*) from entries", (row, index) -> row.getInt(1));
    }

    private DefaultTransactionDefinition definition(TransactionPropagation propagation) {
        return DefaultTransactionDefinition.builder().propagation(propagation).build();
    }

    private JdbcDataSource source() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return source;
    }
}
