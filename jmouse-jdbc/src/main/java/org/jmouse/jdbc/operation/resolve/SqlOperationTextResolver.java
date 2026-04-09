package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;

/**
 * Resolves SQL text for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationTextResolver {

    /**
     * Resolves SQL text for the given operation.
     *
     * @param operation operation instance
     *
     * @return resolved SQL text
     */
    String resolveSql(SqlOperation operation);

}