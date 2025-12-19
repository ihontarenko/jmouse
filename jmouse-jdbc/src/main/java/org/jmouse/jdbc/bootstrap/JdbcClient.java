package org.jmouse.jdbc.bootstrap;

import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.core.JdbcTemplate;
import org.jmouse.jdbc.core.NamedJdbcTemplate;
import org.jmouse.jdbc.dialect.SqlDialect;

/**
 * Fully wired JDBC client facade.
 */
public record JdbcClient(
        JdbcTemplate jdbc,
        JdbcExecutor executor,
        SqlDialect dialect
) {

    public NamedJdbcTemplate getNamedExecutor(MissingParameterPolicy policy) {
        return new NamedJdbcTemplate(jdbc, policy);
    }

}