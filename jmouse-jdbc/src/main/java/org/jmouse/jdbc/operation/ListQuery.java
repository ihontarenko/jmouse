package org.jmouse.jdbc.operation;

import java.util.List;

/**
 * SQL query that returns zero or more rows mapped into a {@link List}.
 *
 * @param <T> mapped row element type
 *
 * @author Ivan Hontarenko
 */
public interface ListQuery<T> extends TypedQuery<T, List<T>> {
}