package org.jmouse.core.context.mutable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 🗂 Map-backed mutable key-value context.
 *
 * <p>
 * Simple {@link Map}-based implementation of {@link MutableKeyValueContext}
 * with predictable iteration order.
 * </p>
 *
 * <p>
 * Provides direct mutation operations while exposing
 * a read-only map view for safe inspection.
 * </p>
 */
public class MapBackedMutableContext implements MutableKeyValueContext {

    /**
     * 🔑 Internal storage.
     */
    private final Map<Object, Object> values;

    /**
     * 🏗 Create empty context.
     */
    public MapBackedMutableContext() {
        this.values = new LinkedHashMap<>();
    }

    /**
     * 🏗 Create context initialized with given values.
     *
     * @param initialValues initial key-value pairs
     */
    public MapBackedMutableContext(Map<Object, Object> initialValues) {
        this.values = new LinkedHashMap<>(Objects.requireNonNull(initialValues, "initialValues"));
    }

    /**
     * 👀 Read-only map view.
     *
     * @return unmodifiable view of internal map
     */
    @Override
    public Map<Object, Object> asMapView() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * 🔍 Get value by key.
     *
     * @param key lookup key
     * @param <T> expected value type
     * @return value or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(Object key) {
        return (T) values.get(key);
    }

    /**
     * ❓ Check key presence.
     *
     * @param key lookup key
     * @return {@code true} if key exists
     */
    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    /**
     * ✏️ Put or replace value.
     *
     * @param key   key
     * @param value value
     */
    @Override
    public void setValue(Object key, Object value) {
        values.put(key, value);
    }

    /**
     * 🧹 Remove value by key.
     *
     * @param key key to remove
     */
    @Override
    public void removeValue(Object key) {
        values.remove(key);
    }

    /**
     * 🔄 Clear all entries.
     */
    @Override
    public void clear() {
        values.clear();
    }

    /**
     * 🧾 Debug-friendly representation.
     */
    @Override
    public String toString() {
        return "ContextValues" + values;
    }
}
