package org.jmouse.core.context.execution;

import org.jmouse.core.context.ContextKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

/**
 * 🧱 Default immutable implementation of {@link ExecutionContext}.
 *
 * <p>
 * {@code DefaultExecutionContext} is a simple, immutable, map-backed
 * execution context implementation. Each modification produces
 * a new context instance, preserving the original context unchanged.
 * </p>
 *
 * <h3>Design characteristics</h3>
 * <ul>
 *   <li><b>Immutability</b> – internal state cannot be modified after creation</li>
 *   <li><b>Structural copy-on-write</b> – {@link #with(ContextKey, Object)} creates a shallow copy</li>
 *   <li><b>Insertion order preserved</b> – backed by {@link LinkedHashMap}</li>
 *   <li><b>Thread-safe by design</b> – immutable instances are safe to share</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ExecutionContext ctx = new DefaultExecutionContext()
 *         .with(ContextKeys.REQUEST_ID, "req-123")
 *         .with(ContextKeys.USER_ID, "u-42");
 *
 * try (var scope = ExecutionContextHolder.open(ctx)) {
 *     String requestId = ExecutionContextHolder.current()
 *             .get(ContextKeys.REQUEST_ID);
 * }
 * }</pre>
 *
 * <h3>Null semantics</h3>
 * <ul>
 *   <li>Passing {@code null} as a value to {@link #with(ContextKey, Object)}
 *       removes the key from the context.</li>
 *   <li>{@link #get(ContextKey)} returns {@code null} when a key is absent.</li>
 * </ul>
 */
public final class DefaultExecutionContext implements ExecutionContext {

    /**
     * Immutable map holding context entries.
     */
    private final Map<ContextKey<?>, Object> entries;

    /**
     * Creates an empty execution context.
     */
    public DefaultExecutionContext() {
        this.entries = Collections.emptyMap();
    }

    /**
     * Creates a new execution context backed by the given entries.
     * <p>
     * The provided map is defensively wrapped as unmodifiable.
     * </p>
     *
     * @param entries context entries
     */
    private DefaultExecutionContext(Map<ContextKey<?>, Object> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }

    /**
     * Returns the value associated with the given context key.
     *
     * @param key typed context key
     * @param <T> value type
     * @return the associated value, or {@code null} if not present
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ContextKey<T> key) {
        nonNull(key, "key");
        Object value = entries.get(key);
        return value == null ? null : (T) value;
    }

    /**
     * Returns a new execution context with the given key–value pair applied.
     * <p>
     * If the value is {@code null}, the key is removed from the context.
     * The original context instance remains unchanged.
     * </p>
     *
     * @param key   typed context key
     * @param value value to associate (may be {@code null})
     * @param <T>   value type
     * @return a new {@link ExecutionContext} instance containing the update
     */
    @Override
    public <T> ExecutionContext with(ContextKey<T> key, T value) {
        nonNull(key, "key");

        Map<ContextKey<?>, Object> copy = new LinkedHashMap<>(entries);

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new DefaultExecutionContext(copy);
    }

    /**
     * Returns all entries contained in this execution context.
     * <p>
     * The returned map is immutable and represents a stable snapshot
     * of the context state.
     * </p>
     *
     * @return immutable map of context entries
     */
    @Override
    public Map<ContextKey<?>, Object> entries() {
        return entries;
    }

    /**
     * Creates an immutable snapshot of this execution context.
     * <p>
     * The snapshot captures the current entries and can be safely
     * transferred across thread boundaries.
     * </p>
     *
     * @return snapshot of this execution context
     */
    @Override
    public ExecutionSnapshot snapshot() {
        return new ExecutionSnapshot(entries);
    }
}
