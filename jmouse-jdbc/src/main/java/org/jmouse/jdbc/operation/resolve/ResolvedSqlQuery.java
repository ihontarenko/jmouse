package org.jmouse.jdbc.operation.resolve;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Resolved form of a typed SQL query.
 *
 * @param <T> mapped row element type
 * @param <R> final query result type
 *
 * @author Ivan Hontarenko
 */
public interface ResolvedSqlQuery<T, R> extends ResolvedSqlOperation {

    /**
     * Returns the mapped row element type.
     *
     * @return mapped row element type
     */
    Class<T> elementType();

    /**
     * Returns the expected query cardinality.
     *
     * @return query cardinality
     */
    QueryCardinality cardinality();

    record Default<T, R>(
            SqlOperation operation,
            String sql,
            ParameterSource parameters,
            Class<T> elementType,
            QueryCardinality cardinality
    ) implements ResolvedSqlQuery<T, R> {

        /**
         * Creates a new resolved SQL query descriptor.
         *
         * @param operation   original operation
         * @param sql         resolved SQL text
         * @param parameters  resolved parameter source
         * @param elementType mapped row element type
         * @param cardinality expected query cardinality
         */
        public Default {
            Verify.nonNull(operation, "operation");
            Verify.nonNull(sql, "sql");
            Verify.nonNull(parameters, "parameters");
            Verify.nonNull(elementType, "elementType");
            Verify.nonNull(cardinality, "cardinality");
        }

    }

}