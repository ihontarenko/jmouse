package org.jmouse.jdbc.core;

import org.jmouse.jdbc.bind.BeanParameterSource;
import org.jmouse.jdbc.bind.MapParameterSource;
import org.jmouse.jdbc.bind.ParameterSource;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NamedOperations extends SimpleOperations {

    default <T> Optional<T> querySingle(String sql, Map<String, ?> parameters, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, new MapParameterSource(parameters), mapper);
    }

    default <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, new BeanParameterSource(bean), mapper);
    }

    default <T> List<T> query(String sql, Map<String, ?> parameters, RowMapper<T> mapper) throws SQLException {
        return query(sql, new MapParameterSource(parameters), mapper);
    }

    default int update(String sql, Map<String, ?> parameters) throws SQLException {
        return update(sql, new MapParameterSource(parameters));
    }

    <T> Optional<T> querySingle(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    <T> T queryOne(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    <T> List<T> query(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    int update(String sql, ParameterSource parameterSource) throws SQLException;

    int[] batch(String sql, List<? extends ParameterSource> parameterSources) throws SQLException;

    <K> K update(String sql, ParameterSource parameterSource, KeyExtractor<K> extractor) throws SQLException;

}
