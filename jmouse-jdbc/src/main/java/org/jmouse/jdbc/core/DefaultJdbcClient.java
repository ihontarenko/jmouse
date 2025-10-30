package org.jmouse.jdbc.core;

import org.jmouse.jdbc.core.mapping.RowMapper;
import org.jmouse.jdbc.errors.EmptyResultException;
import org.jmouse.jdbc.errors.NonUniqueResultException;
import org.jmouse.jdbc.spi.SQLExceptionTranslator;
import org.jmouse.jdbc.tx.ConnectionBinding;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ⚙️ Default template built on Part 0 primitives.
 * Prefers thread-bound {@link Connection} from {@link ConnectionBinding} if present.
 */
public final class DefaultJdbcClient implements JdbcClient {

    private final DataSource             dataSource;
    private final ConnectionBinding      binding;
    private final SQLExceptionTranslator exceptionTranslator;

    public DefaultJdbcClient(DataSource dataSource, ConnectionBinding binding, SQLExceptionTranslator translator) {
        this.dataSource = dataSource;
        this.binding = binding;
        this.exceptionTranslator = translator;
    }

    // -------------- Positional

    @Override
    public int update(String sql, Object... args) {
        return executeUpdate(new Sql(sql), args);
    }

    @Override
    public int updateAndReturnKey(String sql, KeyHolder kh, Object... args) {
        return executeUpdateReturningKey(new Sql(sql, true), kh, args);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
        return executeQuery(new Sql(sql), ParameterBinder.of(args), mapper);
    }

    @Override
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... args) {
        List<T> list = query(sql, mapper, args);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public <T> T queryOneRequired(String sql, RowMapper<T> mapper, Object... args) {
        List<T> list = query(sql, mapper, args);
        if (list.isEmpty()) throw new EmptyResultException("No rows for: " + sql);
        if (list.size() > 1) throw new NonUniqueResultException("Multiple rows for: " + sql);
        return list.get(0);
    }

    // -------------- Named

    @Override
    public int updateNamed(String sql, SqlParameterSource params) {
        NamedSql ns = NamedParameterUtils.parse(sql);
        return update(ns.sql(), NamedParameterUtils.buildArgs(ns, params));
    }

    @Override
    public int updateNamedAndReturnKey(String sql, SqlParameterSource params, KeyHolder keyHolder) {
        NamedSql ns = NamedParameterUtils.parse(sql);
        return updateAndReturnKey(ns.sql(), keyHolder, NamedParameterUtils.buildArgs(ns, params));
    }

    @Override
    public <T> List<T> queryNamed(String sql, SqlParameterSource params, RowMapper<T> mapper) {
        NamedSql ns = NamedParameterUtils.parse(sql);
        return query(ns.sql(), mapper, NamedParameterUtils.buildArgs(ns, params));
    }

    @Override
    public <T> Optional<T> queryOneNamed(String sql, SqlParameterSource params, RowMapper<T> mapper) {
        List<T> list = queryNamed(sql, params, mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public <T> T queryOneNamedRequired(String sql, SqlParameterSource params, RowMapper<T> mapper) {
        List<T> list = queryNamed(sql, params, mapper);
        if (list.isEmpty()) throw new EmptyResultException("No rows for: " + sql);
        if (list.size() > 1) throw new NonUniqueResultException("Multiple rows for: " + sql);
        return list.get(0);
    }

    // -------------- Convenience

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
        return query(sql, new SingleColumnRowMapper<>(elementType), args);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> type, Object... args) {
        return queryOneRequired(sql, new SingleColumnRowMapper<>(type), args);
    }

    // -------------- Batch

    @Override
    public int[] batch(String sql, List<Object[]> batchArgs) {
        Connection c     = null;
        boolean    close = false;
        try {
            c = currentOrAcquire();
            try (PreparedStatement ps = new Sql(sql).prepare(c)) {
                for (Object[] args : batchArgs) {
                    bind(ps, args);
                    ps.addBatch();
                }
                return ps.executeBatch();
            }
        } catch (SQLException e) {
            throw exceptionTranslator.translate("batch", sql, e);
        } finally {
            if (close) closeQuietly(c);
        }
    }

    @Override
    public int[] batchNamed(String sql, List<SqlParameterSource> batchParams) {
        NamedSql       ns       = NamedParameterUtils.parse(sql);
        List<Object[]> argsList = new ArrayList<>(batchParams.size());
        for (SqlParameterSource p : batchParams) {
            argsList.add(NamedParameterUtils.buildArgs(ns, p));
        }
        return batch(ns.sql(), argsList);
    }

    // -------------- Internals

    private int executeUpdate(Sql sql, Object... args) {
        Connection c     = null;
        boolean    close = false;
        try {
            c = currentOrAcquire();
            try (PreparedStatement ps = sql.prepare(c)) {
                bind(ps, args);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw exceptionTranslator.translate("update", sql.text(), e);
        } finally {
            if (close) closeQuietly(c);
        }
    }

    private int executeUpdateReturningKey(Sql sql, KeyHolder kh, Object... args) {
        Connection c     = null;
        boolean    close = false;
        try {
            c = currentOrAcquire();
            try (PreparedStatement ps = sql.prepare(c)) {
                bind(ps, args);
                int updated = ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys != null && keys.next()) {
                        kh.setKey(keys.getObject(1));
                    }
                }
                return updated;
            }
        } catch (SQLException e) {
            throw exceptionTranslator.translate("updateReturningKey", sql.text(), e);
        } finally {
            if (close) closeQuietly(c);
        }
    }

    private <T> List<T> executeQuery(Sql sql, ParameterBinder binder, RowMapper<T> mapper) {
        Connection c     = null;
        boolean    close = false;
        try {
            c = currentOrAcquire();
            try (PreparedStatement ps = sql.prepare(c)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> result = new ArrayList<>();
                    int     i      = 0;
                    while (rs.next()) result.add(mapper.map(rs, i++));
                    return result;
                }
            }
        } catch (SQLException e) {
            throw exceptionTranslator.translate("query", sql.text(), e);
        } finally {
            if (close) closeQuietly(c);
        }
    }

    private void bind(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    private Connection currentOrAcquire() throws SQLException {
        Connection bound = binding.currentConnection();
        if (bound != null) return bound;
        Connection c = dataSource.getConnection();
        return c; // caller closes only if owned — simplified via flag; here we rely on try-with-resource per statement
    }

    private void closeQuietly(Connection c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignore) {
        }
    }
}
