package org.jmouse.jdbc.operation.template;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.NamedOperations;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.operation.ListQuery;
import org.jmouse.jdbc.operation.OptionalQuery;
import org.jmouse.jdbc.operation.SingleQuery;
import org.jmouse.jdbc.operation.SqlUpdate;
import org.jmouse.jdbc.operation.TypedQuery;
import org.jmouse.jdbc.operation.resolve.QueryCardinality;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlOperation;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlQuery;
import org.jmouse.jdbc.operation.resolve.ResolvedSqlUpdate;
import org.jmouse.jdbc.operation.resolve.SqlOperationResolver;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link SqlOperationTemplate}.
 *
 * <p>Delegates execution to {@link NamedOperations} after resolving SQL,
 * parameters, and typed operation metadata.</p>
 */
public class DefaultSqlOperationTemplate implements SqlOperationTemplate {

    private final SqlOperationRowMapperResolver rowMapperResolver;
    private final SqlOperationResolver          resolver;
    private final NamedOperations               operations;
    private final SqlOperationInstantiator      instantiator;

    public DefaultSqlOperationTemplate(
            SqlOperationRowMapperResolver rowMapperResolver,
            NamedOperations operations,
            SqlOperationResolver resolver,
            SqlOperationInstantiator instantiator
    ) {
        this.rowMapperResolver = Verify.nonNull(rowMapperResolver, "rowMapperResolver");
        this.operations = Verify.nonNull(operations, "operations");
        this.resolver = Verify.nonNull(resolver, "resolver");
        this.instantiator = Verify.nonNull(instantiator, "instantiator");
    }

    @Override
    public <T> List<T> query(Class<? extends ListQuery<T>> type) throws SQLException {
        return query(instantiator.instantiate(type));
    }

    @Override
    public <T> List<T> query(ListQuery<T> operation) throws SQLException {
        ResolvedSqlQuery<T, List<T>> resolved = resolver.resolve(operation);
        assertCardinality(resolved, QueryCardinality.LIST);

        RowMapper<T> mapper = resolveRowMapper(operation);

        return operations.query(
                resolved.sql(),
                resolved.parameters(),
                mapper
        );
    }

    @Override
    public <T> Optional<T> queryOptional(Class<? extends OptionalQuery<T>> type) throws SQLException {
        return queryOptional(instantiator.instantiate(type));
    }

    @Override
    public <T> Optional<T> queryOptional(OptionalQuery<T> operation) throws SQLException {
        ResolvedSqlQuery<T, Optional<T>> resolved = resolver.resolve(operation);
        assertCardinality(resolved, QueryCardinality.OPTIONAL);

        RowMapper<T> mapper = resolveRowMapper(operation);

        return operations.querySingle(
                resolved.sql(),
                resolved.parameters(),
                mapper
        );
    }

    @Override
    public <T> T queryOne(Class<? extends SingleQuery<T>> type) throws SQLException {
        return queryOne(instantiator.instantiate(type));
    }

    @Override
    public <T> T queryOne(SingleQuery<T> operation) throws SQLException {
        ResolvedSqlQuery<T, T> resolved = resolver.resolve(operation);
        assertCardinality(resolved, QueryCardinality.SINGLE);

        RowMapper<T> mapper = resolveRowMapper(operation);

        return operations.queryOne(
                resolved.sql(),
                resolved.parameters(),
                mapper
        );
    }

    @Override
    public int update(Class<? extends SqlUpdate> type) throws SQLException {
        return update(instantiator.instantiate(type));
    }

    @Override
    public int update(SqlUpdate operation) throws SQLException {
        ResolvedSqlUpdate resolved = resolver.resolve(operation);

        return operations.update(
                resolved.sql(),
                resolved.parameters()
        );
    }

    protected <T> RowMapper<T> resolveRowMapper(TypedQuery<T, ?> query) {
        return rowMapperResolver.resolveRowMapper(query.elementType());
    }

    protected void assertCardinality(ResolvedSqlOperation resolved, QueryCardinality expected) {
        if (resolved instanceof ResolvedSqlQuery<?, ?> query && query.cardinality() != expected) {
            throw new IllegalStateException(
                    "Resolved query cardinality mismatch. Expected " + expected + " but got " + query.cardinality()
            );
        }
    }

}