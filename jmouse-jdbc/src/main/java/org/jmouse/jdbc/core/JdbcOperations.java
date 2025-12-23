package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JdbcOperations {

    CoreOperations core();

    <T> Optional<T> querySingle(String sql, Map<String, ?> params, RowMapper<T> mapper) throws SQLException;

    <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper) throws SQLException;

    <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> mapper) throws SQLException;

    int update(String sql, Map<String, ?> params) throws SQLException;
}
