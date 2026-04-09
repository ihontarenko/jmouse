package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Resolves a parameter source for a typed SQL operation.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationParametersResolver {

    /**
     * Resolves a parameter source for the given operation.
     *
     * @param operation operation instance
     *
     * @return resolved parameter source
     */
    ParameterSource resolveParameters(SqlOperation operation);

}