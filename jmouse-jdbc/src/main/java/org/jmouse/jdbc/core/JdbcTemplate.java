package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.PreparedStatement;

public class JdbcTemplate implements JdbcOperations {

    private final JdbcExecutor executor;

    public JdbcTemplate(JdbcExecutor executor) {
        this.executor = executor;
    }

    @Override
    public <T> T query(String sql, RowMapper<T> mapper) {
        try {
            return executor.execute(sql, PreparedStatement::executeQuery, rs -> rs.next() ? mapper.map(rs) : null);
        } catch (Exception ignored) {
            return null;
        }
    }

}