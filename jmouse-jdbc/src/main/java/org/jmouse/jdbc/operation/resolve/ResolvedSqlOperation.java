package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Resolved form of a typed SQL operation.
 *
 * <p>A resolved operation contains all metadata required by execution
 * infrastructure, including the original operation instance, resolved SQL text,
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
     * Returns resolved SQL text.
     *
     * @return resolved SQL text
     */
    String sql();

    /**
     * Returns resolved parameter source.
     *
     * @return resolved parameter source
     */
    ParameterSource parameters();

}