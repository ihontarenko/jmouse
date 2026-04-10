package org.jmouse.jdbc.operation.resolve;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.SqlUpdate;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Resolved form of a typed SQL update operation.
 *
 * @author Ivan Hontarenko
 */
public interface ResolvedSqlUpdate extends ResolvedSqlOperation {

    record Default(
            SqlUpdate operation,
            String sql,
            ParameterSource parameters
    ) implements ResolvedSqlUpdate {

        /**
         * Creates a new resolved SQL update descriptor.
         *
         * @param operation  original update operation
         * @param sql        resolved SQL text
         * @param parameters resolved parameter source
         */
        public Default {
            Verify.nonNull(operation, "operation");
            Verify.nonNull(sql, "sql");
            Verify.nonNull(parameters, "parameters");
        }

    }

}