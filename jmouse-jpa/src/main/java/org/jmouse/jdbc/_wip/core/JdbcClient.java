package org.jmouse.jdbc._wip.core;

import org.jmouse.jdbc._wip.core.mapping.RowMapper;
import org.jmouse.jdbc.SqlParameterSource;

import java.util.List;
import java.util.Optional;

/**
 * 🧰 Thin JDBC template with positional and named parameters.
 */
public interface JdbcClient {

    // Positional APIs
    int update(String sql, Object... args);

    int updateAndReturnKey(String sql, KeyHolder keyHolder, Object... args);

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... args);

    <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... args);

    <T> T queryOneRequired(String sql, RowMapper<T> mapper, Object... args);

    // Named parameter APIs
    int updateNamed(String sql, SqlParameterSource params);

    int updateNamedAndReturnKey(String sql, SqlParameterSource params, KeyHolder keyHolder);

    <T> List<T> queryNamed(String sql, SqlParameterSource params, RowMapper<T> mapper);

    <T> Optional<T> queryOneNamed(String sql, SqlParameterSource params, RowMapper<T> mapper);

    <T> T queryOneNamedRequired(String sql, SqlParameterSource params, RowMapper<T> mapper);

    // Convenience
    <T> List<T> queryForList(String sql, Class<T> elementType, Object... args);

    <T> T queryForObject(String sql, Class<T> type, Object... args);

    // Batch
    int[] batch(String sql, List<Object[]> batchArgs);

    int[] batchNamed(String sql, List<SqlParameterSource> batchParams);
}
