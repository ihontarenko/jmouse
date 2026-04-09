package org.jmouse.jdbc.operation.template.support;

import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.parameters.ParameterSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Internal delegate that bridges the declarative operation layer to the
 * existing named-parameter template infrastructure.
 */
public interface NamedDeclaredOperationsDelegate {

    /**
     * Executes a list query.
     */
    <T> List<T> queryList(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException;

    /**
     * Executes an optional query.
     */
    <T> Optional<T> queryOptional(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException;

    /**
     * Executes a required single-result query.
     */
    <T> T querySingle(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException;

    /**
     * Executes an update.
     */
    int update(String sql, ParameterSource parameters) throws SQLException;

}