package org.jmouse.jdbc.operation.template;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.JdbcOperations;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.operation.*;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecution;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecutionFactory;
import org.jmouse.jdbc.operation.resolve.QueryCardinality;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlOperation;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlQuery;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlUpdate;
import org.jmouse.jdbc.operation.resolve.SqlOperationResolver;
import org.jmouse.jdbc.statement.StatementOptions;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Declarative template for executing typed SQL operations.
 *
 * <p>This contract provides a high-level operation-oriented facade over the
 * existing JDBC template layer.</p>
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationTemplate {

    /**
     * Executes a parameterless list query operation by its type.
     *
     * @param operationType operation type
     * @param <T>           mapped row element type
     *
     * @return mapped result list
     *
     * @throws SQLException if execution fails
     */
    <T> List<T> query(Class<? extends ListQuery<T>> operationType) throws SQLException;

    /**
     * Executes a list query operation instance.
     *
     * @param operation operation instance
     * @param <T>       mapped row element type
     *
     * @return mapped result list
     *
     * @throws SQLException if execution fails
     */
    <T> List<T> query(ListQuery<T> operation) throws SQLException;

    /**
     * Executes a parameterless optional query operation by its type.
     *
     * @param operationType operation type
     * @param <T>           mapped row element type
     *
     * @return mapped optional result
     *
     * @throws SQLException if execution fails
     */
    <T> Optional<T> queryOptional(Class<? extends OptionalQuery<T>> operationType) throws SQLException;

    /**
     * Executes an optional query operation instance.
     *
     * @param operation operation instance
     * @param <T>       mapped row element type
     *
     * @return mapped optional result
     *
     * @throws SQLException if execution fails
     */
    <T> Optional<T> queryOptional(OptionalQuery<T> operation) throws SQLException;

    /**
     * Executes a parameterless required single-result query operation by its type.
     *
     * @param operationType operation type
     * @param <T>           mapped row element type
     *
     * @return mapped result
     *
     * @throws SQLException if execution fails
     */
    <T> T queryOne(Class<? extends SingleQuery<T>> operationType) throws SQLException;

    /**
     * Executes a required single-result query operation instance.
     *
     * @param operation operation instance
     * @param <T>       mapped row element type
     *
     * @return mapped result
     *
     * @throws SQLException if execution fails
     */
    <T> T queryOne(SingleQuery<T> operation) throws SQLException;

    /**
     * Executes a parameterless update operation by its type.
     *
     * @param operationType operation type
     *
     * @return affected row count
     *
     * @throws SQLException if execution fails
     */
    int update(Class<? extends SqlUpdate> operationType) throws SQLException;

    /**
     * Executes an update operation instance.
     *
     * @param operation operation instance
     *
     * @return affected row count
     *
     * @throws SQLException if execution fails
     */
    int update(SqlUpdate operation) throws SQLException;

    /**
     * Default {@link SqlOperationTemplate} implementation.
     *
     * <p>This template resolves typed SQL operations, prepares executable SQL,
     * resolves statement options, and delegates execution to the base
     * {@link JdbcOperations} contract.</p>
     */
    final class Default implements SqlOperationTemplate {

        private final SqlOperationRowMapperResolver    rowMapperResolver;
        private final SqlOperationResolver             resolver;
        private final JdbcOperations                   operations;
        private final SqlOperationInstantiator         instantiator;
        private final NamedSqlPreparedExecutionFactory preparedExecutionFactory; // no class found
        private final StatementOptionsResolver         statementOptionsResolver;

        /**
         * Creates a new SQL operation template.
         *
         * @param rowMapperResolver       row mapper resolver
         * @param operations              JDBC operations facade
         * @param resolver                operation resolver
         * @param instantiator            operation instantiator for class-based execution
         * @param preparedExecutionFactory prepared SQL execution factory
         * @param statementOptionsResolver statement options resolver
         */
        public Default(
                SqlOperationRowMapperResolver rowMapperResolver,
                JdbcOperations operations,
                SqlOperationResolver resolver,
                SqlOperationInstantiator instantiator,
                NamedSqlPreparedExecutionFactory preparedExecutionFactory,
                StatementOptionsResolver statementOptionsResolver
        ) {
            this.rowMapperResolver = Verify.nonNull(rowMapperResolver, "rowMapperResolver");
            this.operations = Verify.nonNull(operations, "operations");
            this.resolver = Verify.nonNull(resolver, "resolver");
            this.instantiator = Verify.nonNull(instantiator, "instantiator");
            this.preparedExecutionFactory = Verify.nonNull(preparedExecutionFactory, "preparedExecutionFactory");
            this.statementOptionsResolver = Verify.nonNull(statementOptionsResolver, "statementOptionsResolver");
        }

        @Override
        public <T> List<T> query(Class<? extends ListQuery<T>> operationType) throws SQLException {
            return query(instantiator.instantiate(operationType));
        }

        @Override
        public <T> List<T> query(ListQuery<T> operation) throws SQLException {
            ResolvedSqlQuery<T, List<T>> resolved = resolver.resolve(operation);
            assertCardinality(resolved, QueryCardinality.LIST);

            NamedSqlPreparedExecution prepared = prepare(resolved);
            RowMapper<T>              mapper   = resolveRowMapper(operation);
            StatementOptions          options  = resolveStatementOptions(operation);

            return operations.query(
                    prepared.sql(),
                    prepared.binder(),
                    options.statementConfigurer(),
                    options.queryStatementHandler(),
                    mapper
            );
        }

        @Override
        public <T> Optional<T> queryOptional(Class<? extends OptionalQuery<T>> operationType) throws SQLException {
            return queryOptional(instantiator.instantiate(operationType));
        }

        @Override
        public <T> Optional<T> queryOptional(OptionalQuery<T> operation) throws SQLException {
            ResolvedSqlQuery<T, Optional<T>> resolved = resolver.resolve(operation);
            assertCardinality(resolved, QueryCardinality.OPTIONAL);

            NamedSqlPreparedExecution prepared = prepare(resolved);
            StatementOptions options = resolveStatementOptions(operation);
            RowMapper<T> mapper = resolveRowMapper(operation);

            return operations.querySingle(
                    prepared.sql(),
                    prepared.binder(),
                    options.statementConfigurer(),
                    options.queryStatementHandler(),
                    mapper
            );
        }

        @Override
        public <T> T queryOne(Class<? extends SingleQuery<T>> operationType) throws SQLException {
            return queryOne(instantiator.instantiate(operationType));
        }

        @Override
        public <T> T queryOne(SingleQuery<T> operation) throws SQLException {
            ResolvedSqlQuery<T, T> resolved = resolver.resolve(operation);
            assertCardinality(resolved, QueryCardinality.SINGLE);

            NamedSqlPreparedExecution prepared = prepare(resolved);
            StatementOptions          options  = resolveStatementOptions(operation);
            RowMapper<T>              mapper   = resolveRowMapper(operation);

            return operations.queryOne(
                    prepared.sql(),
                    prepared.binder(),
                    options.statementConfigurer(),
                    options.queryStatementHandler(),
                    mapper
            );
        }

        @Override
        public int update(Class<? extends SqlUpdate> operationType) throws SQLException {
            return update(instantiator.instantiate(operationType));
        }

        @Override
        public int update(SqlUpdate operation) throws SQLException {
            ResolvedSqlUpdate         resolved = resolver.resolve(operation);
            NamedSqlPreparedExecution prepared = prepare(resolved);
            StatementOptions          options  = resolveStatementOptions(operation);

            return operations.update(
                    prepared.sql(),
                    prepared.binder(),
                    options.statementConfigurer(),
                    options.updateStatementHandler()
            );
        }

        /**
         * Prepares executable SQL and binder for the resolved operation.
         *
         * @param resolved resolved operation
         *
         * @return prepared execution descriptor
         */
        private NamedSqlPreparedExecution prepare(ResolvedSqlOperation resolved) {
            return preparedExecutionFactory.prepare(resolved.sql(), resolved.parameters());
        }

        /**
         * Resolves statement execution options for the given operation.
         *
         * @param operation original operation
         *
         * @return resolved statement options
         */
        private StatementOptions resolveStatementOptions(SqlOperation operation) {
            return statementOptionsResolver.resolve(operation);
        }

        /**
         * Resolves row mapper for the given typed query.
         *
         * @param query typed query
         * @param <T>   mapped row element type
         *
         * @return resolved row mapper
         */
        private <T> RowMapper<T> resolveRowMapper(TypedQuery<T, ?> query) {
            return rowMapperResolver.resolveRowMapper(query.elementType());
        }

        /**
         * Verifies resolved query cardinality against the expected execution path.
         *
         * @param resolved resolved operation descriptor
         * @param expected expected cardinality
         */
        private void assertCardinality(ResolvedSqlOperation resolved, QueryCardinality expected) {
            if (resolved instanceof ResolvedSqlQuery<?, ?> query && query.cardinality() != expected) {
                throw new IllegalStateException(
                        "Resolved query cardinality mismatch. Expected " + expected + " but got " + query.cardinality()
                );
            }
        }

    }

}