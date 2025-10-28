package org.jmouse.jdbc.spi;

import org.jmouse.jdbc.errors.JdbcException;

import java.sql.SQLException;

/**
 * 🧠 Translates vendor-specific {@link SQLException} to {@code RuntimeException}.
 */
@FunctionalInterface
public interface SQLExceptionTranslator {

    RuntimeException translate(String task, String sql, SQLException exception);

    static SQLExceptionTranslator passthrough() {
        return (task, sql, exception)
                -> new JdbcException("%s failed: %s".formatted(task, sql), exception);
    }
}