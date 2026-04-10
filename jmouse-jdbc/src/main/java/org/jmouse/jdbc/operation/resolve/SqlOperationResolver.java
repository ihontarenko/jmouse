package org.jmouse.jdbc.operation.resolve;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.operation.*;
import org.jmouse.jdbc.operation.annotation.InlineSql;
import org.jmouse.jdbc.operation.annotation.SqlResource;
import org.jmouse.jdbc.operation.source.InlineSqlSource;
import org.jmouse.jdbc.operation.source.ResourceSqlSource;
import org.jmouse.jdbc.operation.source.SqlSource;
import org.jmouse.jdbc.parameters.ParameterSource;

import java.util.List;
import java.util.Optional;

/**
 * Resolves typed SQL operations into execution-ready descriptors.
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationResolver {

    /**
     * Resolves the given operation into a generic resolved descriptor.
     *
     * @param operation operation instance
     *
     * @return resolved operation descriptor
     */
    ResolvedSqlOperation resolve(SqlOperation operation);

    /**
     * Resolves the given list query.
     *
     * @param operation query operation instance
     * @param <T>       mapped row element type
     * @param <Q>       query type
     *
     * @return resolved query descriptor
     */
    <T, Q extends ListQuery<T>> ResolvedSqlQuery<T, List<T>> resolve(Q operation);

    /**
     * Resolves the given optional query.
     *
     * @param operation query operation instance
     * @param <T>       mapped row element type
     * @param <Q>       query type
     *
     * @return resolved query descriptor
     */
    <T, Q extends OptionalQuery<T>> ResolvedSqlQuery<T, Optional<T>> resolve(Q operation);

    /**
     * Resolves the given single-result query.
     *
     * @param operation query operation instance
     * @param <T>       mapped row element type
     * @param <Q>       query type
     *
     * @return resolved query descriptor
     */
    <T, Q extends SingleQuery<T>> ResolvedSqlQuery<T, T> resolve(Q operation);

    /**
     * Resolves the given update operation.
     *
     * @param operation update operation instance
     *
     * @return resolved update descriptor
     */
    ResolvedSqlUpdate resolve(SqlUpdate operation);

    class Default implements SqlOperationResolver {

        private final SqlOperationParametersResolver parametersResolver;
        private final SqlTextLoader                  textLoader;

        /**
         * Creates a new resolver.
         *
         * @param parametersResolver parameter source resolver
         * @param textLoader         SQL text loader for resource-based SQL sources
         */
        public Default(
                SqlOperationParametersResolver parametersResolver,
                SqlTextLoader textLoader
        ) {
            this.parametersResolver = Verify.nonNull(parametersResolver, "parametersResolver");
            this.textLoader = Verify.nonNull(textLoader, "textLoader");
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

            return new ResolvedSqlQuery.Default<>(
                    operation,
                    resolveSql(operation),
                    resolveParameters(operation),
                    operation.elementType(),
                    QueryCardinality.LIST
            );
        }

        @Override
        public <T, Q extends OptionalQuery<T>> ResolvedSqlQuery<T, Optional<T>> resolve(Q operation) {
            Verify.nonNull(operation, "operation");

            return new ResolvedSqlQuery.Default<>(
                    operation,
                    resolveSql(operation),
                    resolveParameters(operation),
                    operation.elementType(),
                    QueryCardinality.OPTIONAL
            );
        }

        @Override
        public <T, Q extends SingleQuery<T>> ResolvedSqlQuery<T, T> resolve(Q operation) {
            Verify.nonNull(operation, "operation");

            return new ResolvedSqlQuery.Default<>(
                    operation,
                    resolveSql(operation),
                    resolveParameters(operation),
                    operation.elementType(),
                    QueryCardinality.SINGLE
            );
        }

        @Override
        public ResolvedSqlUpdate resolve(SqlUpdate operation) {
            Verify.nonNull(operation, "operation");

            return new ResolvedSqlUpdate.Default(
                    operation,
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

        /**
         * Resolves SQL text for the given operation.
         *
         * @param operation operation instance
         *
         * @return resolved SQL text
         */
        protected String resolveSql(SqlOperation operation) {
            Verify.nonNull(operation, "operation");

            if (operation instanceof SqlSourceOperation sourceOperation) {
                return resolveSource(Verify.nonNull(sourceOperation.sqlSource(), "sqlSource"));
            }

            Class<?> operationType = operation.getClass();

            InlineSql inlineSql = operationType.getAnnotation(InlineSql.class);
            if (inlineSql != null) {
                return Verify.notBlank(inlineSql.value(), "inlineSql");
            }

            SqlResource resource = operationType.getAnnotation(SqlResource.class);
            if (resource != null) {
                return textLoader.load(Verify.notBlank(resource.value(), "sqlResource"));
            }

            throw new IllegalStateException(
                    "No SQL source defined for operation: " + operation.getClass().getName()
            );
        }

        /**
         * Resolves SQL text from an explicit SQL source.
         *
         * @param source SQL source
         *
         * @return resolved SQL text
         */
        protected String resolveSource(SqlSource source) {
            if (source instanceof InlineSqlSource(String value)) {
                return Verify.notBlank(value, "sql");
            }

            if (source instanceof ResourceSqlSource(String value)) {
                return textLoader.load(Verify.notBlank(value, "resource"));
            }

            throw new IllegalStateException("Unsupported SQL source type: " + source.getClass().getName());
        }

        /**
         * Resolves parameter source for the given operation.
         *
         * @param operation operation instance
         *
         * @return resolved parameter source
         */
        protected ParameterSource resolveParameters(SqlOperation operation) {
            return parametersResolver.resolveParameters(operation);
        }

    }

}