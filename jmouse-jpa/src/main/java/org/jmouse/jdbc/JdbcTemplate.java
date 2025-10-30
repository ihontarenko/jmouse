package org.jmouse.jdbc;

import org.jmouse.jdbc.spi.SQLExceptionTranslator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 🛠️ Low-level JDBC executor.
 */
public final class JdbcTemplate {

    private final ConnectionProvider connections;
    private final SQLExceptionTranslator errors;

    public JdbcTemplate(ConnectionProvider connections,
                        SQLExceptionTranslator errors) {
        this.connections = connections;
        this.errors = errors;
    }

    public int update(String sql, Object... params) {
        try (JdbcConnection jc = connections.acquire();
             PreparedStatement ps = prepare(jc, sql, false)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw errors.translate("update", sql, e);
        }
    }

    public int updateAndReturnKeys(String sql, KeyHolder keyHolder, Object... params) {
        try (JdbcConnection jc = connections.acquire();
             PreparedStatement ps = prepare(jc, sql, true)) {
            bind(ps, params);
            int n = ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs != null && rs.next()) {
                    keyHolder.setKey(rs.getObject(1));
                }
            }
            return n;
        } catch (SQLException e) {
            throw errors.translate("insert", sql, e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (JdbcConnection jc = connections.acquire();
             PreparedStatement ps = prepare(jc, sql, false)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                int i = 0;
                while (rs.next()) {
                    result.add(mapper.map(rs, i++));
                }
                return result;
            }
        } catch (SQLException e) {
            throw errors.translate("query", sql, e);
        }
    }

    public int[] batch(String sql, List<Object[]> batch) {
        try (JdbcConnection jc = connections.acquire();
             PreparedStatement ps = prepare(jc, sql, false)) {
            for (Object[] params : batch) {
                bind(ps, params);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw errors.translate("batch", sql, e);
        }
    }

    private PreparedStatement prepare(JdbcConnection jc, String sql, boolean keys) throws SQLException {
        return keys
                ? jc.unwrap().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : jc.unwrap().prepareStatement(sql);
    }

    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
