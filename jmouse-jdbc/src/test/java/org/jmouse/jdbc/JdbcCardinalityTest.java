package org.jmouse.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jmouse.jdbc.connection.DataSourceConnectionProvider;
import org.jmouse.jdbc.exception.EmptyResultException;
import org.jmouse.jdbc.exception.NonUniqueResultException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JdbcCardinalityTest {
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate.Default(new SQLExecutor(new DataSourceConnectionProvider(source)));
        jdbc.update("create table entries (id integer primary key)");
    }

    @Test
    void optionalQueryReturnsEmptyForNoRows() throws Exception {
        assertTrue(jdbc.querySingle("select id from entries", (row, index) -> row.getInt(1)).isEmpty());
    }

    @Test
    void requiredQueryRejectsNoRows() {
        assertThrows(EmptyResultException.class,
                () -> jdbc.queryOne("select id from entries", (row, index) -> row.getInt(1)));
    }

    @Test
    void bothSingleRowContractsRejectMultipleRows() throws Exception {
        jdbc.update("insert into entries values (1), (2)");
        assertThrows(NonUniqueResultException.class,
                () -> jdbc.querySingle("select id from entries", (row, index) -> row.getInt(1)));
        assertThrows(NonUniqueResultException.class,
                () -> jdbc.queryOne("select id from entries", (row, index) -> row.getInt(1)));
    }

    @Test
    void requiredQueryKeepsNullableMapperBehaviorForOneRow() throws Exception {
        jdbc.update("insert into entries values (1)");
        assertNull(jdbc.queryOne("select id from entries", (row, index) -> null));
    }
}
