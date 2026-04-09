package org.jmouse.jdbc.operation;

/**
 * SQL query that is expected to return exactly one mapped result.
 *
 * <p>The concrete execution policy for zero rows or multiple rows is defined
 * by the template and extractor infrastructure.</p>
 *
 * @param <T> mapped row element type
 *
 * @author Ivan Hontarenko
 */
public interface SingleQuery<T> extends TypedQuery<T, T> {
}