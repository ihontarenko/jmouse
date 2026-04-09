package org.jmouse.jdbc.operation.template;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.NamedOperations;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.operation.*;
import org.jmouse.jdbc.operation.resolve.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DefaultSqlOperationTemplate implements SqlOperationTemplate {

    private final SqlOperationRowMapperResolver mapperResolver;
    private final SqlOperationResolver          resolver;
    private final NamedOperations               operations;
    private final SqlOperationInstantiator      instantiator;

    public DefaultSqlOperationTemplate(
            SqlOperationRowMapperResolver mapperResolver, NamedOperations operations,
            SqlOperationResolver resolver,
            SqlOperationInstantiator instantiator
    ) {
        this.mapperResolver = Verify.nonNull(mapperResolver, "rowMapperResolver");
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
        ResolvedSqlQuery<T, List<T>> resolution = resolver.resolve(operation);
        RowMapper<T>                 mapper     = resolveRowMapper(operation);

        return operations.query(
                resolution.sql(),
                resolution.parameters(),
                mapper
        );
    }

    @Override
    public <T> Optional<T> queryOptional(Class<? extends OptionalQuery<T>> type) throws SQLException {
        return queryOptional(instantiator.instantiate(type));
    }

    @Override
    public <T> Optional<T> queryOptional(OptionalQuery<T> operation) throws SQLException {
        ResolvedSqlQuery<T, Optional<T>> resolution = resolver.resolve(operation);
        RowMapper<T>                     mapper     = resolveRowMapper(operation);

        return operations.querySingle(
                resolution.sql(),
                resolution.parameters(),
                mapper
        );
    }

    @Override
    public <T> T queryOne(Class<? extends SingleQuery<T>> type) throws SQLException {
        return queryOne(instantiator.instantiate(type));
    }

    @Override
    public <T> T queryOne(SingleQuery<T> operation) throws SQLException {
        ResolvedSqlQuery<T, T> resolution = resolver.resolve(operation);
        RowMapper<T>           mapper     = resolveRowMapper(operation);

        return operations.queryOne(
                resolution.sql(),
                resolution.parameters(),
                mapper
        );
    }

    @Override
    public int update(Class<? extends SqlUpdate> type) throws SQLException {
        return update(instantiator.instantiate(type));
    }

    @Override
    public int update(SqlUpdate operation) throws SQLException {
        ResolvedSqlUpdate resolution = resolver.resolve(operation);
        return operations.update(
                resolution.sql(),
                resolution.parameters()
        );
    }

    protected <T> RowMapper<T> resolveRowMapper(TypedQuery<T, ?> query) {
        return mapperResolver.resolveRowMapper(query.elementType());
    }

    protected void assertCardinality(ResolvedSqlOperation resolved, QueryCardinality expected) {
        if (resolved instanceof ResolvedSqlQuery<?, ?> query && query.cardinality() != expected) {
            throw new IllegalStateException(
                    "Resolved query cardinality mismatch. Expected " + expected + " but got " + query.cardinality()
            );
        }
    }

}