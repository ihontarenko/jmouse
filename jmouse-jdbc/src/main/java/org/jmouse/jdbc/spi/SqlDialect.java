package org.jmouse.jdbc.spi;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 🧭 Small dialect hook if needed later (identity retrieval, pagination, etc.).
 */
public interface SqlDialect {

    default String currentTimestampFunction() {
        return "CURRENT_TIMESTAMP";
    }

    default boolean supportsBatch() {
        return true;
    }

    default void configure(Connection connection) throws SQLException {

    }

}