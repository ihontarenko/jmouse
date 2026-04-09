package org.jmouse.jdbc.operation.template.support;

import org.jmouse.jdbc.NamedOperations;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.parameters.ParameterSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;

/**
 * Adapter from the existing {@link org.jmouse.jdbc.NamedTemplate} API to
 * {@link NamedDeclaredOperationsDelegate}.
 */
public class NamedTemplateDeclaredOperationsDelegate implements NamedDeclaredOperationsDelegate {

    private final NamedOperations template;

    public NamedTemplateDeclaredOperationsDelegate(NamedOperations template) {
        this.template = nonNull(template, "template");
    }

    @Override
    public <T> List<T> queryList(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException {
        return template.query(sql, parameters, rowMapper);
    }

    @Override
    public <T> Optional<T> queryOptional(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException {
        return template.querySingle(sql, parameters, rowMapper);
    }

    @Override
    public <T> T querySingle(String sql, ParameterSource parameters, RowMapper<T> rowMapper) throws SQLException {
        return template.queryOne(sql, parameters, rowMapper);
    }

    @Override
    public int update(String sql, ParameterSource parameters) throws SQLException {
        return template.update(sql, parameters);
    }

}