package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Resolved form of a typed SQL operation.
 *
 * <p>A resolved operation contains all metadata required by execution
 * infrastructure, including the operation instance, logical name, SQL text,
 * and parameter source.</p>
 *
 * @author Ivan Hontarenko
 */
public interface ResolvedSqlOperation {

    /**
     * Returns the original operation instance.
     *
     * @return original operation instance
     */
    SqlOperation operation();

    /**
     * Returns the logical operation name.
     *
     * @return logical operation name
     */
    String name();

    /**
     * Returns the resolved SQL text.
     *
     * @return resolved SQL text
     */
    String sql();

    /**
     * Returns the resolved parameter source.
     *
     * @return resolved parameter source
     */
    ParameterSource parameters();

}