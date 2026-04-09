package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.resolve.SqlOperationParametersResolver;
import org.jmouse.jdbc.parameters.BeanParameterSource;
import org.jmouse.jdbc.parameters.EmptyParameterSource;
import org.jmouse.jdbc.parameters.ParameterSource;

/**
 * Default {@link SqlOperationParametersResolver} implementation.
 *
 * <p>Parameterless operations use {@link EmptyParameterSource}. All other
 * operations are exposed through {@link BeanParameterSource}.</p>
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationParametersResolver implements SqlOperationParametersResolver {

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