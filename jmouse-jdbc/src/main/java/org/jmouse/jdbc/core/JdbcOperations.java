package org.jmouse.jdbc.core;

import org.jmouse.jdbc.mapping.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JdbcOperations {

    CoreOperations core();

    <T> Optional<T> querySingle(String sql, Map<String, ?> params, RowMapper<T> mapper);

    <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper);

    <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> mapper);

    int update(String sql, Map<String, ?> params);
}
