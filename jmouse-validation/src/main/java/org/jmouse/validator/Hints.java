package org.jmouse.validator;

import java.util.Arrays;
import java.util.List;

/**
 * Lightweight immutable container for contextual hints used during validation. 🧩
 *
 * <p>
 * {@code Hints} is a generic mechanism for passing additional metadata
 * into validation or processing logic without coupling components directly.
 * </p>
 *
 * <p>
 * Typical use cases include:
 * </p>
 * <ul>
 *     <li>Operation markers (create/update)</li>
 *     <li>Feature flags</li>
 *     <li>Role indicators</li>
 *     <li>Custom validation switches</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * Hints hints = Hints.of(Operation.CREATE, new AdminFlag());
 *
 * if (hints.contains(Operation.CREATE)) {
 *     // apply create-specific logic
 * }
 *
 * if (hints.has(AdminFlag.class)) {
 *     AdminFlag flag = hints.find(AdminFlag.class);
 * }
 * }</pre>
 *
 * <p>
 * This record is immutable and thread-safe provided that contained values
 * are themselves immutable.
 * </p>
 *
 * @param values hint objects (may be empty but never {@code null} by convention)
 */
public record Hints(List<Object> values) {

    /**
     * Returns an empty hint container.
     *
     * @return empty hints instance
     */
    public static Hints empty() {
        return new Hints(List.of());
    }

    /**
     * Creates a hint container from provided objects.
     *
     * @param hints hint values
     * @return new hints instance
     */
    public static Hints of(Object... hints) {
        if (hints == null || hints.length == 0) {
            return empty();
        }
        return new Hints(Arrays.asList(hints));
    }

    /**
     * @return {@code true} if no hints are present
     */
    public boolean isEmpty() {
        return values == null || values.isEmpty();
    }

    /**
     * Checks whether a hint of the given type exists.
     *
     * @param type type to search for
     * @return {@code true} if any stored value is an instance of the type
     */
    public boolean has(Class<?> type) {
        if (values == null) {
            return false;
        }

        for (Object value : values) {
            if (type.isInstance(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first hint matching the given type.
     *
     * @param type target type
     * @param <T>  generic type
     * @return matching hint or {@code null} if none found
     */
    public <T> T find(Class<T> type) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }

        return null;
    }

    /**
     * Checks whether the given hint object is present.
     *
     * @param hint hint to check
     * @return {@code true} if contained
     */
    public boolean contains(Object hint) {
        return values != null && values.contains(hint);
    }

}