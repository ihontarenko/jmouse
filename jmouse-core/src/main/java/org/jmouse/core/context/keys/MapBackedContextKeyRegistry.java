package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🗂 Map-backed context key registry.
 *
 * <p>
 * Default {@link MutableContextKeyRegistry} implementation
 * supporting key lookup by:
 * </p>
 * <ul>
 *   <li>👤 user class</li>
 *   <li>📦 concrete value (identity-based)</li>
 * </ul>
 *
 * <p>
 * Class-based keys use logical equality,
 * value-based keys use <b>identity semantics</b>.
 * </p>
 */
final class MapBackedContextKeyRegistry implements MutableContextKeyRegistry {

    /**
     * 👤 Keys associated with user classes.
     */
    private final Map<Class<?>, ContextKey<?>> classKeys;

    /**
     * 📦 Keys associated with concrete values (identity-based).
     */
    private final Map<Object, ContextKey<?>>   valueKeys;

    private MapBackedContextKeyRegistry() {
        this.classKeys = new LinkedHashMap<>();
        this.valueKeys = new IdentityHashMap<>();
    }

    /**
     * 🏗 Create empty registry.
     */
    static MapBackedContextKeyRegistry create() {
        return new MapBackedContextKeyRegistry();
    }

    /**
     * 🔍 Find key registered for a specific value.
     *
     * @param value source value
     * @return matching key or {@code null}
     */
    @Override
    public ContextKey<?> findKeyForValue(Object value) {
        if (value == null) {
            return null;
        }
        return valueKeys.get(value);
    }

    /**
     * 🔍 Find key registered for a user class.
     *
     * @param userClass user-level class
     * @return matching key or {@code null}
     */
    @Override
    public ContextKey<?> findKeyForUserClass(Class<?> userClass) {
        if (userClass == null) {
            return null;
        }
        return classKeys.get(userClass);
    }

    /**
     * ➕ Register key for user class.
     *
     * @param userClass user-level class
     * @param key       context key
     * @return this registry
     */
    @Override
    public MutableContextKeyRegistry registerForUserClass(Class<?> userClass, ContextKey<?> key) {
        Verify.nonNull(userClass, "userClass");
        Verify.nonNull(key, "key");
        classKeys.put(userClass, key);
        return this;
    }

    /**
     * ➕ Register key for concrete value.
     *
     * <p>
     * Uses identity comparison ({@code ==}).
     * </p>
     *
     * @param value concrete value
     * @param key   context key
     * @return this registry
     */
    @Override
    public MutableContextKeyRegistry registerForValue(Object value, ContextKey<?> key) {
        Verify.nonNull(value, "value");
        Verify.nonNull(key, "key");
        valueKeys.put(value, key);
        return this;
    }

    /**
     * 🧾 Debug-friendly representation.
     */
    @Override
    public String toString() {
        return "MapBackedContextKeyRegistry[userClasses=" + classKeys.size()
                + ", values=" + valueKeys.size() + "]";
    }
}
