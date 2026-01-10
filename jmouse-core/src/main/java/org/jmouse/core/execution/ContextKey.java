package org.jmouse.core.execution;

import static org.jmouse.core.Verify.nonNull;

/**
 * 🏷 Typed key used to access values from an {@link ExecutionContext}.
 *
 * <p>
 * {@code ContextKey} represents a strongly-typed identifier for values
 * stored inside an {@link ExecutionContext}. It combines a stable string
 * identifier with a value {@link Class type} to provide compile-time
 * type safety when accessing context entries.
 * </p>
 *
 * <h3>Design goals</h3>
 * <ul>
 *   <li><b>Type safety</b> – prevents accidental type mismatches</li>
 *   <li><b>Explicit identity</b> – keys are compared by instance identity</li>
 *   <li><b>Stable semantics</b> – string ID is intended for debugging and logging</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * public static final ContextKey<String> REQUEST_ID =
 *         ContextKey.of("requestId", String.class);
 *
 * ExecutionContext ctx = new DefaultExecutionContext()
 *         .with(REQUEST_ID, "req-123");
 *
 * String requestId = ctx.get(REQUEST_ID);
 * }</pre>
 *
 * <h3>Equality and identity</h3>
 * <p>
 * {@code ContextKey} does not override {@link #equals(Object)} or {@link #hashCode()}.
 * Keys are therefore compared by <em>instance identity</em>.
 * </p>
 *
 * <p>
 * This design encourages declaring keys as {@code static final} constants
 * and avoids accidental key collisions across modules.
 * </p>
 *
 * @param <T> value type associated with this key
 */
public final class ContextKey<T> {

    /**
     * Stable identifier of this key (used for diagnostics and logging).
     */
    private final String id;

    /**
     * Runtime type of the value associated with this key.
     */
    private final Class<T> type;

    private ContextKey(String id, Class<T> type) {
        this.id = nonNull(id, "id");
        this.type = nonNull(type, "type");
    }

    /**
     * Creates a new typed context key.
     *
     * @param id   stable identifier (should be unique within the application)
     * @param type value type associated with this key
     * @param <T>  value type
     * @return a new {@link ContextKey} instance
     */
    public static <T> ContextKey<T> of(String id, Class<T> type) {
        return new ContextKey<>(id, type);
    }

    /**
     * Returns the identifier of this key.
     *
     * @return key identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the value type associated with this key.
     *
     * @return value type
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Returns a human-readable representation of this key.
     *
     * @return string representation of the key
     */
    @Override
    public String toString() {
        return "ContextKey[%s]".formatted(id);
    }
}
