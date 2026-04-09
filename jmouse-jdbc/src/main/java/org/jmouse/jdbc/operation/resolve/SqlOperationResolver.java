package org.jmouse.jdbc.operation.resolve;

import org.jmouse.jdbc.operation.ListQuery;
import org.jmouse.jdbc.operation.OptionalQuery;
import org.jmouse.jdbc.operation.SingleQuery;
import org.jmouse.jdbc.operation.SqlOperation;
import org.jmouse.jdbc.operation.SqlUpdate;

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

}