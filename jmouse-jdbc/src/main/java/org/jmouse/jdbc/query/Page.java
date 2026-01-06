package org.jmouse.jdbc.query;

import java.util.List;

/**
 * Immutable container representing a single page of query results.
 * <p>
 * {@code Page} encapsulates both the data slice and pagination metadata,
 * allowing callers to reason about navigation and total size without
 * coupling to a specific SQL dialect or pagination strategy.
 *
 * <h3>Semantics</h3>
 * <ul>
 *     <li>{@code items}   – the current page items</li>
 *     <li>{@code offset}  – starting row offset used to fetch this page</li>
 *     <li>{@code limit}   – maximum number of rows requested</li>
 *     <li>{@code hasNext} – whether a subsequent page is available</li>
 *     <li>{@code total}   – total number of rows (optional, may be {@code null})</li>
 * </ul>
 *
 * <p>
 * The {@code total} field is optional to support databases or queries where
 * an additional {@code COUNT(*)} is expensive or intentionally omitted.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Page<User> page = Page.of(
 *     users,
 *     offset,
 *     limit,
 *     users.size() == limit,
 *     totalCount
 * );
 *
 * if (page.hasNext()) {
 *     // fetch next page
 * }
 * }</pre>
 *
 * <p>
 * The {@link #of(List, long, int, boolean, Long)} factory defensively copies
 * the item list to guarantee immutability.
 *
 * @param items   page items
 * @param offset  starting offset (0-based)
 * @param limit   page size
 * @param hasNext {@code true} if another page exists
 * @param total   total row count, or {@code null} if unknown
 *
 * @author jMouse
 */
public record Page<T>(
        List<T> items,
        long offset,
        int limit,
        boolean hasNext,
        Long total
) {

    /**
     * Factory method for creating an immutable {@link Page}.
     *
     * @param items   page items
     * @param offset  starting offset (0-based)
     * @param limit   page size
     * @param hasNext {@code true} if another page exists
     * @param total   total row count, or {@code null} if unknown
     * @param <T>     item type
     * @return immutable page instance
     */
    public static <T> Page<T> of(List<T> items, long offset, int limit, boolean hasNext, Long total) {
        return new Page<>(List.copyOf(items), offset, limit, hasNext, total);
    }
}
