package org.jmouse.jdbc.execution;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.jdbc.JdbcTemplate;
import org.jmouse.jdbc.NamedTemplate;
import org.jmouse.jdbc.mapping.ResultSetExtractor;
import org.jmouse.jdbc.statement.StatementHandler;

import java.sql.SQLException;

public interface JdbcDescriptorOperations extends NamedTemplate, JdbcTemplate {

    @SuppressWarnings("unchecked")
    default <T> T queryOperation(ExecutionDescriptor descriptor) throws SQLException {

        switch (descriptor.operation()) {
            case QUERY -> {}
            case UPDATE -> {}
            case CALL -> {}
            case UPDATE_RETURNING_KEYS -> {}
            case BATCH_UPDATE -> {}
        }

        return query(
                descriptor.sql(), descriptor.binder(),
                descriptor.statementConfigurer(),
                Reflections.cast(descriptor.statementHandler(), StatementHandler.class),
                (ResultSetExtractor<T>) descriptor.resultSetExtractor()
        );
    }

}
