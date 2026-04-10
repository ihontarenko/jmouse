package org.jmouse.jdbc;

import org.jmouse.jdbc.parameters.ParameterSource;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.parameters.SQLCompiled;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecution;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecutionFactory;
import org.jmouse.jdbc.statement.StatementBinder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC template variant supporting named SQL parameters.
 *
 * <p>Delegates SQL preparation to {@link NamedSqlPreparedExecutionFactory},
 * converting named parameters into JDBC {@code ?} placeholders and binders.</p>
 */
public final class NamedTemplate extends JdbcTemplate implements NamedOperations {

    private final NamedSqlPreparedExecutionFactory executionFactory;

    /**
     * Creates a named-parameter JDBC template.
     *
     * @param executor JDBC executor
     * @param executionFactory factory for SQL preparation and binding
     */
    public NamedTemplate(
            JdbcExecutor executor,
            NamedSqlPreparedExecutionFactory executionFactory
    ) {
        super(executor);
        this.executionFactory = executionFactory;
    }

    /**
     * Executes a query expecting zero or one result.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return optional result
     */
    @Override
    public <T> Optional<T> querySingle(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException {
        NamedSqlPreparedExecution preparedExecution = executionFactory.prepare(sql, parameterSource);
        StatementBinder           binder            = preparedExecution.binder();
        return super.querySingle(preparedExecution.sql(), binder, mapper);
    }

    /**
     * Executes a query expecting exactly one result.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return mapped result
     */
    @Override
    public <T> T queryOne(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException {
        NamedSqlPreparedExecution preparedExecution = executionFactory.prepare(sql, parameterSource);
        StatementBinder           binder            = preparedExecution.binder();
        return super.queryOne(preparedExecution.sql(), binder, mapper);
    }

    /**
     * Executes a query returning multiple results.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return list of mapped results
     */
    @Override
    public <T> List<T> query(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException {
        NamedSqlPreparedExecution preparedExecution = executionFactory.prepare(sql, parameterSource);
        StatementBinder           binder            = preparedExecution.binder();
        return super.query(preparedExecution.sql(), binder, mapper);
    }

    /**
     * Executes an update statement.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @return number of affected rows
     */
    @Override
    public int update(String sql, ParameterSource parameterSource) throws SQLException {
        NamedSqlPreparedExecution preparedExecution = executionFactory.prepare(sql, parameterSource);
        StatementBinder           binder            = preparedExecution.binder();
        return super.update(preparedExecution.sql(), binder);
    }

    /**
     * Executes batch update using the same SQL and multiple parameter sets.
     *
     * <p>SQL is compiled once and reused for all parameter sources.</p>
     *
     * @param sql SQL with named parameters
     * @param parameterSources list of parameter sources
     * @return update counts per batch entry
     */
    @Override
    public int[] batch(String sql, List<? extends ParameterSource> parameterSources) throws SQLException {
        SQLCompiled           compiled = executionFactory.compile(sql);
        List<StatementBinder> binders  = new ArrayList<>(parameterSources.size());

        for (ParameterSource parameterSource : parameterSources) {
            binders.add(executionFactory.binder(compiled, parameterSource));
        }

        return super.batchUpdate(compiled.compiled(), binders);
    }

    /**
     * Executes an update and extracts generated keys.
     *
     * @param sql SQL with named parameters
     * @param parameters parameter source
     * @param extractor key extractor
     * @return extracted key
     */
    @Override
    public <K> K update(String sql, ParameterSource parameters, KeyExtractor<K> extractor) throws SQLException {
        NamedSqlPreparedExecution preparedExecution = executionFactory.prepare(sql, parameters);
        StatementBinder           binder            = preparedExecution.binder();
        return super.update(preparedExecution.sql(), binder, extractor);
    }

}