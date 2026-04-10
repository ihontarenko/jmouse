package org.jmouse.jdbc;

import org.jmouse.jdbc.parameters.BeanParameterSource;
import org.jmouse.jdbc.parameters.MapParameterSource;
import org.jmouse.jdbc.parameters.ParameterSource;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extension of {@link JdbcOperations} for SQL with named parameters.
 *
 * <p>Supports {@link ParameterSource} directly, as well as convenience overloads
 * for {@link Map} and bean-based parameter binding.</p>
 */
public interface NamedOperations extends JdbcOperations {

    /**
     * Executes a query expecting zero or one result using map-based parameters.
     *
     * @param sql SQL with named parameters
     * @param parameters parameter map
     * @param mapper row mapper
     * @return optional mapped result
     * @throws SQLException if JDBC access fails
     */
    default <T> Optional<T> querySingle(String sql, Map<String, ?> parameters, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, new MapParameterSource(parameters), mapper);
    }

    /**
     * Executes a query expecting zero or one result using bean-based parameters.
     *
     * @param sql SQL with named parameters
     * @param bean parameter bean
     * @param mapper row mapper
     * @return optional mapped result
     * @throws SQLException if JDBC access fails
     */
    default <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper) throws SQLException {
        return querySingle(sql, new BeanParameterSource(bean), mapper);
    }

    /**
     * Executes a query returning multiple results using map-based parameters.
     *
     * @param sql SQL with named parameters
     * @param parameters parameter map
     * @param mapper row mapper
     * @return list of mapped results
     * @throws SQLException if JDBC access fails
     */
    default <T> List<T> query(String sql, Map<String, ?> parameters, RowMapper<T> mapper) throws SQLException {
        return query(sql, new MapParameterSource(parameters), mapper);
    }

    /**
     * Executes an update using map-based parameters.
     *
     * @param sql SQL with named parameters
     * @param parameters parameter map
     * @return number of affected rows
     * @throws SQLException if JDBC access fails
     */
    default int update(String sql, Map<String, ?> parameters) throws SQLException {
        return update(sql, new MapParameterSource(parameters));
    }

    /**
     * Executes a query expecting zero or one result.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return optional mapped result
     * @throws SQLException if JDBC access fails
     */
    <T> Optional<T> querySingle(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    /**
     * Executes a query expecting exactly one result.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return mapped result
     * @throws SQLException if JDBC access fails
     */
    <T> T queryOne(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    /**
     * Executes a query returning multiple results.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param mapper row mapper
     * @return list of mapped results
     * @throws SQLException if JDBC access fails
     */
    <T> List<T> query(String sql, ParameterSource parameterSource, RowMapper<T> mapper) throws SQLException;

    /**
     * Executes an update statement.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @return number of affected rows
     * @throws SQLException if JDBC access fails
     */
    int update(String sql, ParameterSource parameterSource) throws SQLException;

    /**
     * Executes a batch update using multiple parameter sources.
     *
     * @param sql SQL with named parameters
     * @param parameterSources parameter sources for batch entries
     * @return update counts per batch entry
     * @throws SQLException if JDBC access fails
     */
    int[] batch(String sql, List<? extends ParameterSource> parameterSources) throws SQLException;

    /**
     * Executes an update and extracts generated keys.
     *
     * @param sql SQL with named parameters
     * @param parameterSource parameter source
     * @param extractor generated key extractor
     * @return extracted key result
     * @throws SQLException if JDBC access fails
     */
    <K> K update(String sql, ParameterSource parameterSource, KeyExtractor<K> extractor) throws SQLException;

}