package org.jmouse.jdbc.core;

import org.jmouse.jdbc.bind.*;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NamedJdbcTemplate {

    private final JdbcTemplate        jdbc;
    private final NamedBinderCompiler compiler;

    public NamedJdbcTemplate(JdbcTemplate jdbc, MissingParameterPolicy missingPolicy) {
        this.jdbc = jdbc;
        this.compiler = new NamedBinderCompiler(missingPolicy);
    }

    public <T> Optional<T> querySingle(String sql, Map<String, ?> params, RowMapper<T> mapper) throws SQLException {
        CompiledNamedSQL compiled = compiler.compile(sql);
        return jdbc.querySingle(compiled.sql(), compiler.binder(compiled, new MapSqlParameterSource(params)), mapper);
    }

    public <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper) throws SQLException {
        CompiledNamedSQL compiled = compiler.compile(sql);
        return jdbc.querySingle(compiled.sql(), compiler.binder(compiled, new BeanSqlParameterSource(bean)), mapper);
    }

    public <T> List<T> query(String sql, Map<String, ?> parameters, RowMapper<T> mapper) throws SQLException {
        CompiledNamedSQL compiled = compiler.compile(sql);
        return jdbc.query(compiled.sql(), compiler.binder(compiled, new MapSqlParameterSource(parameters)), mapper);
    }

    public int update(String sql, Map<String, ?> parameters) throws SQLException {
        CompiledNamedSQL compiled = compiler.compile(sql);
        return jdbc.update(compiled.sql(), compiler.binder(compiled, new MapSqlParameterSource(parameters)));
    }
}
