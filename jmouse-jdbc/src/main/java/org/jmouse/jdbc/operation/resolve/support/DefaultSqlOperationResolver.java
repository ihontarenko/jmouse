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
 *
 * @author Ivan Hontarenko
 */
public class DefaultSqlOperationResolver implements SqlOperationResolver {

    private final SqlOperationNameResolver       nameResolver;
    private final SqlOperationTextResolver       textResolver;
    private final SqlOperationParametersResolver parametersResolver;

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

        if (operation instanceof ListQuery<?> query) {
            return resolveListUnchecked(query);
        }

        if (operation instanceof OptionalQuery<?> query) {
            return resolveOptionalUnchecked(query);
        }

        if (operation instanceof SingleQuery<?> query) {
            return resolveSingleUnchecked(query);
        }

        if (operation instanceof SqlUpdate update) {
            return resolve(update);
        }

        throw new IllegalStateException("Unsupported SQL operation type: " + operation.getClass().getName());
    }

    @Override
    public <T, Q extends ListQuery<T>> ResolvedSqlQuery<T, List<T>> resolve(Q operation) {
        Verify.nonNull(operation, "operation");

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
        Verify.nonNull(operation, "operation");

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
        Verify.nonNull(operation, "operation");

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
        Verify.nonNull(operation, "operation");

        return new DefaultResolvedSqlUpdate(
                operation,
                resolveName(operation),
                resolveSql(operation),
                resolveParameters(operation)
        );
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveListUnchecked(ListQuery<?> operation) {
        return resolve((ListQuery<Object>) operation);
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveOptionalUnchecked(OptionalQuery<?> operation) {
        return resolve((OptionalQuery<Object>) operation);
    }

    @SuppressWarnings("unchecked")
    protected ResolvedSqlOperation resolveSingleUnchecked(SingleQuery<?> operation) {
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