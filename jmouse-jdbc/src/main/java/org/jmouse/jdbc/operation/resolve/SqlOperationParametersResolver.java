package org.jmouse.jdbc.operation.resolve;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.parameters.BeanParameterSource;
import org.jmouse.jdbc.parameters.EmptyParameterSource;
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

    class Default implements SqlOperationParametersResolver {

        @Override
        public ParameterSource resolveParameters(SqlOperation operation) {
            Verify.nonNull(operation, "operation");

            if (isParameterless(operation)) {
                return EmptyParameterSource.INSTANCE;
            }

            return new BeanParameterSource(operation);
        }

        protected boolean isParameterless(SqlOperation operation) {
            Class<?> operationType = operation.getClass();

            if (operationType.isRecord()) {
                return operationType.getRecordComponents().length == 0;
            }

            return operationType.getDeclaredFields().length == 0;
        }

    }

}