package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.jdbc.operation.SqlUpdate;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlUpdate;
import org.jmouse.jdbc.parameters.ParameterSource;

import static org.jmouse.core.Verify.nonNull;

/**
 * Default immutable {@link ResolvedSqlUpdate} implementation.
 *
 * @author Ivan Hontarenko
 */
public record DefaultResolvedSqlUpdate(
        SqlUpdate operation,
        String name,
        String sql,
        ParameterSource parameters
) implements ResolvedSqlUpdate {

    public DefaultResolvedSqlUpdate {
        nonNull(operation, "operation");
        nonNull(name, "name");
        nonNull(sql, "sql");
        nonNull(parameters, "parameters");
    }

}