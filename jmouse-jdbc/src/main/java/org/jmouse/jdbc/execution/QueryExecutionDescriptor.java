package org.jmouse.jdbc.execution;

import org.jmouse.jdbc.mapping.ListResultSetExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.statement.StatementBinder;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementHandler;

/**
 * 📦 Query execution descriptor.
 *
 * <p>Defines how a query should be executed and mapped.</p>
 *
 * <pre>{@code
 * QueryExecutionDescriptor<User> d = new QueryExecutionDescriptor.Default<>(
 *     "select * from users",
 *     binder,
 *     configurer,
 *     handler,
 *     userMapper,
 *     null
 * );
 * }</pre>
 *
 * @param <T> result type
 */
public interface QueryExecutionDescriptor<T> extends ExecutionDescriptor {

    /**
     * Row mapper (used for row-based mapping).
     */
    RowMapper<T> rowMapper();

    /**
     * Low-level extractor (optional alternative to mapper).
     */
    default ResultSetExtractor<?> resultSetExtractor() {
        return new ListResultSetExtractor<>(rowMapper());
    }

    /**
     * 🧱 Default immutable implementation.
     */
    final class Default<T> implements QueryExecutionDescriptor<T> {

        private final String                sql;
        private final StatementConfigurer   configurer;
        private final StatementBinder       binder;
        private final StatementHandler<?>   handler;
        private final RowMapper<T>          mapper;
        private final ResultSetExtractor<T> extractor;

        /**
         * Creates query descriptor.
         *
         * <p>Either mapper OR extractor must be provided.</p>
         */
        public Default(
                String sql,
                StatementBinder binder,
                StatementConfigurer configurer,
                StatementHandler<?> handler,
                RowMapper<T> mapper,
                ResultSetExtractor<T> extractor
        ) {
            this.sql = sql;
            this.binder = binder;
            this.configurer = configurer;
            this.handler = handler;
            this.mapper = mapper;
            this.extractor = extractor;
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public StatementBinder binder() {
            return binder;
        }

        @Override
        public StatementConfigurer statementConfigurer() {
            return configurer;
        }

        @Override
        public StatementHandler<?> statementHandler() {
            return handler;
        }

        @Override
        public RowMapper<T> rowMapper() {
            return mapper;
        }

        @Override
        public ResultSetExtractor<T> resultSetExtractor() {
            return extractor;
        }
    }
}