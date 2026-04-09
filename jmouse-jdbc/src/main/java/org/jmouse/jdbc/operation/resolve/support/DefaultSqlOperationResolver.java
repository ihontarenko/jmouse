package org.jmouse.jdbc.operation.resolve.support;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.ListQuery;
import org.jmouse.jdbc.operation.OptionalQuery;
import org.jmouse.jdbc.operation.SingleQuery;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.SqlUpdate;
import org.jmouse.jdbc.operation.resolve.*;
import org.jmouse.jdbc.parameters.ParameterSource;

import java.util.List;
import java.util.Optional;

/**
 * Default {@link SqlOperationResolver} implementation.
 */
public class DefaultSqlOperationResolver implements SqlOperationResolver {

    private final SqlOperationNameResolver       nameResolver;
    private final SqlOperationParametersResolver parametersResolver;
    private final SqlOperationTextResolver       textResolver;

    public DefaultSqlOperationResolver(
            SqlOperationNameResolver nameResolver,
            SqlOperationTextResolver textResolver,
            SqlOperationParametersResolver parametersResolver
    ) {
        this.nameResolver = Verify.nonNull(nameResolver, "nameResolver");
        this.textResolver = Verify.nonNull(textResolver, "textResolver");
        this.parametersResolver = Verify.nonNull(parametersResolver, "parametersResolver");
    }

    @Override
    public ResolvedSqlOperation resolve(SqlOperation operation) {
        Verify.nonNull(operation, "operation");

        if (operation instanceof ListQuery<?> listQuery) {
            return resolveUncheckedList(listQuery);
        }

        if (operation instanceof OptionalQuery<?> optionalQuery) {
            return resolveUncheckedOptional(optionalQuery);
        }

        if (operation instanceof SingleQuery<?> singleQuery) {
            return resolveUncheckedSingle(singleQuery);
        }

        if (operation instanceof SqlUpdate update) {
            return resolve(update);
        }

        throw new IllegalStateException("Unsupported SQL operation type: " + operation.getClass().getName());
    }

    @Override
    public <T, Q extends ListQuery<T>> ResolvedSqlQuery<T, List<T>> resolve(Q operation) {
        return new DefaultResolvedSqlQuery<>(
                operation,
                resolveName(operation),
                resolveSql(operation),
                resolveParameters(operation),
                operation.elementType(),
                QueryCardinality.LIST
        );
    }

    @Override
    public <T, Q extends OptionalQuery<T>> ResolvedSqlQuery<T, Optional<T>> resolve(Q operation) {
        return new DefaultResolvedSqlQuery<>(
                operation,
                resolveName(operation),
                resolveSql(operation),
                resolveParameters(operation),
                operation.elementType(),
                QueryCardinality.OPTIONAL
        );
    }

    @Override
    public <T, Q extends SingleQuery<T>> ResolvedSqlQuery<T, T> resolve(Q operation) {
        return new DefaultResolvedSqlQuery<>(
                operation,
                resolveName(operation),
                resolveSql(operation),
                resolveParameters(operation),
                operation.elementType(),
                QueryCardinality.SINGLE
        );
    }

    @Override
    public ResolvedSqlUpdate resolve(SqlUpdate operation) {
        return new DefaultResolvedSqlUpdate(
                operation,
                resolveName(operation),
                resolveSql(operation),
                resolveParameters(operation)
        );
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveUncheckedList(ListQuery<?> operation) {
        return resolve((ListQuery<Object>) operation);
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveUncheckedOptional(OptionalQuery<?> operation) {
        return resolve((OptionalQuery<Object>) operation);
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveUncheckedSingle(SingleQuery<?> operation) {
        return resolve((SingleQuery<Object>) operation);
    }

    protected String resolveName(SqlOperation operation) {
        return nameResolver.resolveName(operation);
    }

    protected String resolveSql(SqlOperation operation) {
        return textResolver.resolveSql(operation);
    }

    protected ParameterSource resolveParameters(SqlOperation operation) {
        return parametersResolver.resolveParameters(operation);
    }

}