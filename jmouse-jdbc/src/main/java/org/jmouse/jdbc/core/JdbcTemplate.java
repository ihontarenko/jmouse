package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.ListResultSetExtractor;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.mapping.SingleResultSetExtractor;
import org.jmouse.jdbc.statement.PreparedStatementBinder;
import org.jmouse.jdbc.statement.QueryStatementCallback;
import org.jmouse.jdbc.statement.UpdateStatementCallback;

import java.sql.SQLException;
import java.util.List;

public final class JdbcTemplate implements JdbcOperations {

    private static final PreparedStatementBinder NO_BINDER = stmt -> {
    };

    private final QueryStatementCallback  queryCallback  = new QueryStatementCallback();
    private final JdbcExecutor            executor;
    private final UpdateStatementCallback updateCallback = new UpdateStatementCallback();

    public JdbcTemplate(JdbcExecutor executor) {
        this.executor = executor;
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper) throws SQLException {
        return query(sql, NO_BINDER, new SingleResultSetExtractor<>(mapper));
    }

    @Override
    public <T> T queryOne(String sql, PreparedStatementBinder binder, RowMapper<T> mapper) throws SQLException {
        return query(sql, binder, new SingleResultSetExtractor<>(mapper));
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper) throws SQLException {
        return query(sql, NO_BINDER, new ListResultSetExtractor<>(mapper));
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
        // We reuse the same executor pipeline for all query paths.
        return executor.execute(sql, binder, queryCallback, extractor);
    }

    @Override
    public int update(String sql) throws SQLException {
        return executor.executeUpdate(sql);
    }

    @Override
    public int update(String sql, PreparedStatementBinder binder) throws SQLException {
        return executor.executeUpdate(sql, binder);
    }
}
