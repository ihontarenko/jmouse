package org.jmouse.jdbc.operation.resolve;

/**
 * Resolved form of a typed SQL query.
 *
 * @param <T> mapped row element type
 * @param <R> final query result type
 *
 * @author Ivan Hontarenko
 */
public interface ResolvedSqlQuery<T, R> extends ResolvedSqlOperation {

    /**
     * Returns the mapped row element type.
     *
     * @return mapped row element type
     */
    Class<T> elementType();

    /**
     * Returns the expected query cardinality.
     *
     * @return query cardinality
     */
    QueryCardinality cardinality();

}