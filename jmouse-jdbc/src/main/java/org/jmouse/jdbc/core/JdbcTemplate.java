package org.jmouse.jdbc.core;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.bind.*;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Named-parameter JDBC template built on top of {@link CoreOperations}.
 */
public final class JdbcTemplate implements JdbcOperations {

    private final CoreOperations      core;
    private final NamedBinderCompiler compiler;

    public JdbcTemplate(CoreOperations core, MissingParameterPolicy missingPolicy) {
        this.core = Contract.nonNull(core, "core");
        this.compiler = new NamedBinderCompiler(Contract.nonNull(missingPolicy, "missingPolicy"));
    }

    @Override
    public CoreOperations core() {
        return core;
    }

    @Override
    public <T> Optional<T> querySingle(String sql, Map<String, ?> params, RowMapper<T> mapper) throws SQLException {
        NamedSQL compiled = compiler.compile(sql);
        return core.querySingle(
                compiled.parsed(),
                compiler.binder(compiled, new MapSqlParameterSource(params)),
                mapper
        );
    }

    @Override
    public <T> Optional<T> querySingle(String sql, Object bean, RowMapper<T> mapper) throws SQLException {
        NamedSQL compiled = compiler.compile(sql);
        return core.querySingle(
                compiled.parsed(),
                compiler.binder(compiled, new BeanSqlParameterSource(bean)),
                mapper
        );
    }

    @Override
    public <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> mapper) throws SQLException {
        NamedSQL compiled = compiler.compile(sql);
        return core.query(
                compiled.parsed(),
                compiler.binder(compiled, new MapSqlParameterSource(params)),
                mapper
        );
    }

    @Override
    public int update(String sql, Map<String, ?> params) throws SQLException {
        NamedSQL compiled = compiler.compile(sql);
        return core.update(
                compiled.parsed(),
                compiler.binder(compiled, new MapSqlParameterSource(params))
        );
    }
}
