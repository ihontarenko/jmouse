package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;

/**
 * Resolves the logical name of a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationNameResolver {

    /**
     * Resolves the logical operation name.
     *
     * @param operation operation instance
     *
     * @return logical operation name
     */
    String resolveName(SqlOperation operation);

}