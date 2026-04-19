package org.jmouse.jdbc.execution;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.jdbc.JdbcTemplate;
import org.jmouse.jdbc.NamedTemplate;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;

public interface JdbcDescriptorOperations extends NamedTemplate, JdbcTemplate {

    @SuppressWarnings("unchecked")
    default <T> T query(QueryExecutionDescriptor<T> descriptor) throws SQLException {
        return descriptor.rowMapper() == null ? query(
                descriptor.sql(),
                descriptor.binder(),
                descriptor.statementConfigurer(),
                Reflections.cast(descriptor.statementHandler(), StatementHandler.class),
                (ResultSetExtractor<T>) descriptor.resultSetExtractor()
        ) : (T) query(
                descriptor.sql(),
                descriptor.binder(),
                descriptor.statementConfigurer(),
                Reflections.cast(descriptor.statementHandler(), StatementHandler.class),
                descriptor.rowMapper()
        );
    }

    @SuppressWarnings("unchecked")
    default <T> T query(Class<? extends QueryExecutionDescriptor<T>> type) throws SQLException {
        return query(
                (QueryExecutionDescriptor<T>) Reflections.instantiate(
                        Reflections.findFirstConstructor(type)
                )
        );
    }

}
