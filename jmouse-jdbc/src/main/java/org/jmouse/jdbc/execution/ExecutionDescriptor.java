package org.jmouse.jdbc.execution;

import org.jmouse.jdbc.JdbcOperation;
import org.jmouse.jdbc.mapping.ListResultSetExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * 📦 Runtime execution descriptor.
 *
 * <p>Represents a fully prepared JDBC execution unit.</p>
 *
 * <pre>{@code
 * ExecutionDescriptor descriptor = ...
 * jdbc.execute(descriptor);
 * }</pre>
 */
public interface ExecutionDescriptor {

    JdbcOperation operation();

    /**
     * Final SQL (ready for JDBC).
     */
    default String sql() {
        return null;
    }

    /**
     * Parameter binder.
     */
    default StatementBinder binder() {
        return StatementBinder.noop();
    }

    /**
     * Statement configuration (timeouts, fetch size, etc).
     */
    default StatementConfigurer statementConfigurer() {
        return StatementConfigurer.noop();
    }

    /**
     * Statement lifecycle hook.
     */
    default StatementHandler<?> statementHandler() {
        return StatementHandler.noop();
    }

    /**
     * Low-level extractor (optional alternative to mapper).
     */
    default ResultSetExtractor<?> resultSetExtractor() {
        return new ListResultSetExtractor<>(
                rowMapper()
        );
    }

    RowMapper<?> rowMapper();

}