package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.*;
import org.jmouse.jdbc.statement.CallableCallback;
import org.jmouse.jdbc.statement.CallableStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.QueryStatementCallback;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SimpleTemplate implements SimpleOperations {

    private static final PreparedStatementBinder NO_BINDER      = statement -> {
    };
    private static final CallableStatementBinder NO_CALL_BINDER = statement -> {
    };

    private final JdbcExecutor           executor;
    private final QueryStatementCallback queryCallback = new QueryStatementCallback();

    public SimpleTemplate(JdbcExecutor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Optional<T> querySingle(String sql, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, NO_BINDER, mapper);
    }

    @Override
    public <T> Optional<T> querySingle(String sql, PreparedStatementBinder binder, RowMapper<T> mapper)
            throws SQLException {
        return executor.execute(sql, binder, queryCallback, new StrictSingleResultSetExtractor<>(mapper, sql));
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper) throws SQLException {
        return queryOne(sql, NO_BINDER, mapper);
    }

    @Override
    public <T> T queryOne(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException {
        return query(sql, binder, new SingleResultSetExtractor<>(mapper));
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper) throws SQLException {
        return query(sql, NO_BINDER, mapper);
    }

    @Override
    public <T> List<T> query(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException {
        return query(sql, binder, new ListResultSetExtractor<>(mapper));
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> extractor) throws SQLException {
        return query(sql, NO_BINDER, extractor);
    }

    @Override
    public <T> T query(String sql, PreparedStatementBinder binder, ResultSetExtractor<T> extractor)
            throws SQLException {
        return executor.execute(sql, binder, queryCallback, extractor);
    }

    @Override
    public int update(String sql) throws SQLException {
        return update(sql, NO_BINDER);
    }

    @Override
    public int update(String sql, PreparedStatementBinder binder) throws SQLException {
        return executor.executeUpdate(sql, binder);
    }

    @Override
    public int[] batchUpdate(String sql, List<? extends PreparedStatementBinder> binders) throws SQLException {
        return executor().executeBatch(sql, binders);
    }

    @Override
    public <K> K update(String sql, PreparedStatementBinder binder, KeyExtractor<K> extractor)
            throws SQLException {
        return executor().executeUpdate(sql, binder, extractor);
    }

    @Override
    public <T> T call(String sql, CallableStatementBinder binder, CallableCallback<T> callback) throws SQLException {
        return executor.executeCall(sql, binder, callback);
    }

    @Override
    public <T> T call(String sql, CallableCallback<T> callback) throws SQLException {
        return call(sql, NO_CALL_BINDER, callback);
    }

    protected JdbcExecutor executor() {
        return executor;
    }
}
