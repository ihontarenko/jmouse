package org.jmouse.jdbc.operation.template;

import org.jmouse.jdbc.operation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Declarative template for executing typed SQL operations.
 *
 * <p>This is a high-level facade over {@code NamedOperations} that allows
 * execution using strongly-typed operation classes.</p>
 *
 * @author Ivan Hontarenko
 */
public interface SqlOperationTemplate {

    <T> List<T> query(Class<? extends ListQuery<T>> operationType) throws SQLException;

    <T> List<T> query(ListQuery<T> operation) throws SQLException;

    <T> Optional<T> queryOptional(Class<? extends OptionalQuery<T>> operationType) throws SQLException;

    <T> Optional<T> queryOptional(OptionalQuery<T> operation) throws SQLException;

    <T> T queryOne(Class<? extends SingleQuery<T>> operationType) throws SQLException;

    <T> T queryOne(SingleQuery<T> operation) throws SQLException;

    int update(Class<? extends SqlUpdate> operationType) throws SQLException;

    int update(SqlUpdate operation) throws SQLException;

}