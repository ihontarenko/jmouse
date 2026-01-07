package org.jmouse.jdbc.query;

import static org.jmouse.core.Verify.state;

/**
 * Value object representing offset/limit pagination parameters.
 * <p>
 * {@code OffsetLimit} models the most common pagination strategy used by
 * SQL databases, where:
 * <ul>
 *     <li>{@code offset} defines how many rows to skip</li>
 *     <li>{@code limit} defines the maximum number of rows to return</li>
 * </ul>
 *
 * <p>
 * Validation is enforced at construction time:
 * <ul>
 *     <li>{@code offset} must be {@code >= 0}</li>
 *     <li>{@code limit} must be {@code > 0}</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * OffsetLimit page = OffsetLimit.of(20, 10);
 *
 * // semantic: skip 20 rows, return up to 10 rows
 * }</pre>
 *
 * <p>
 * This type is typically consumed by SQL dialects or query builders
 * to render database-specific pagination clauses.
 *
 * @param offset number of rows to skip (0-based)
 * @param limit  maximum number of rows to return
 *
 * @author jMouse
 */
public record OffsetLimit(long offset, int limit) {

    /**
     * Canonical constructor with validation.
     */
    public OffsetLimit {
        state(offset >= 0, "offset must be >= 0");
        state(limit > 0, "limit must be > 0");
    }

    /**
     * Factory method for creating an {@link OffsetLimit} instance.
     *
     * @param offset number of rows to skip (0-based)
     * @param limit  maximum number of rows to return
     * @return offset/limit instance
     */
    public static OffsetLimit of(long offset, int limit) {
        return new OffsetLimit(offset, limit);
    }
}
