package org.jmouse.jdbc.operation;

/**
 * Base contract for a typed SQL query whose result is derived from row elements
 * of type {@code T}.
 *
 * <p>This abstraction is used by query specializations such as list, optional,
 * and single-result queries. It exposes the target row element type needed by
 * row-mapping infrastructure.</p>
 *
 * @param <T> mapped row element type
 * @param <R> final query result type
 *
 * @author Ivan Hontarenko
 */
public interface TypedQuery<T, R> extends SqlQuery<R> {

    /**
     * Returns the mapped row element type.
     *
     * <p>This type is used by template infrastructure to resolve an appropriate
     * row mapper, for example a scalar mapper or bean mapper.</p>
     *
     * @return mapped row element type
     */
    Class<T> elementType();

}