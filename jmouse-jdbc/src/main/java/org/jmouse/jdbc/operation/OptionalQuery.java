package org.jmouse.jdbc.operation;

import java.util.Optional;

/**
 * SQL query that returns at most one row mapped into an {@link Optional}.
 *
 * @param <T> mapped row element type
 *
 * @author Ivan Hontarenko
 */
public interface OptionalQuery<T> extends TypedQuery<T, Optional<T>> {
}