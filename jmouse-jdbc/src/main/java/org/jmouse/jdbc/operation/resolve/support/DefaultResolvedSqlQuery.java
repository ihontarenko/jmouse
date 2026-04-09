package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.resolve.QueryCardinality;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlQuery;
import org.jmouse.jdbc.parameters.ParameterSource;

import static org.jmouse.core.Verify.nonNull;

/**
 * Default immutable {@link ResolvedSqlQuery} implementation.
 *
 * @param <T> mapped row element type
 * @param <R> final query result type
 *
 * @author Ivan Hontarenko
 */
public record DefaultResolvedSqlQuery<T, R>(
        SqlOperation operation,
        String name,
        String sql,
        ParameterSource parameters,
        Class<T> elementType,
        QueryCardinality cardinality
) implements ResolvedSqlQuery<T, R> {

    public DefaultResolvedSqlQuery {
        nonNull(operation, "operation");
        nonNull(name, "name");
        nonNull(sql, "sql");
        nonNull(parameters, "parameters");
        nonNull(elementType, "elementType");
        nonNull(cardinality, "cardinality");
    }

}