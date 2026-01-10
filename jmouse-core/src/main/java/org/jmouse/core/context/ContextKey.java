package org.jmouse.core.context;

import org.jmouse.core.context.execution.ExecutionContext;

import static org.jmouse.core.Verify.nonNull;

/**
 * 🗝️ Strongly-typed key for {@link ExecutionContext}.
 *
 * <p>
 * {@code ContextKey} binds a <b>stable name</b> with a <b>value type</b>,
 * enabling type-safe access to context values.
 * </p>
 *
 * <h3>Why this exists</h3>
 * <ul>
 *   <li>🛡 Type safety at access time</li>
 *   <li>🧭 Explicit identity (instance-based)</li>
 *   <li>🧾 Clear diagnostics via stable name</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * static final ContextKey<String> REQUEST_ID =
 *         ContextKey.of("requestId", String.class);
 *
 * ctx.with(REQUEST_ID, "req-42");
 * }</pre>
 *
 * <p>
 * ⚠️ Equality is based on <b>instance identity</b>.
 * Declare keys as {@code static final}.
 * </p>
 *
 * @param <T> value type
 */
public final class ContextKey<T> {

    /**
     * 🏷 Stable logical name (for logs & diagnostics).
     */
    private final String name;

    /**
     * 🧬 Runtime value type.
     */
    private final Class<T> type;

    private ContextKey(String name, Class<T> type) {
        this.name = nonNull(name, "name");
        this.type = nonNull(type, "type");
    }

    /**
     * 🏗 Create a new typed context key.
     *
     * @param name stable logical name
     * @param type associated value type
     * @param <T>  value type
     * @return new {@link ContextKey}
     */
    public static <T> ContextKey<T> of(String name, Class<T> type) {
        return new ContextKey<>(name, type);
    }

    /**
     * 🔠 Uppercase identifier (diagnostic-friendly).
     *
     * @return normalized key id
     */
    public String id() {
        return name.toUpperCase();
    }

    /**
     * 🏷 Raw key name.
     *
     * @return key name
     */
    public String name() {
        return name;
    }

    /**
     * 🧬 Value runtime type.
     *
     * @return value class
     */
    public Class<T> type() {
        return type;
    }

    /**
     * 🧾 Human-readable form.
     */
    @Override
    public String toString() {
        return "ContextKey[%s]".formatted(name);
    }
}
