package org.jmouse.core.access.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Base {@link DataProvider} implementation backed by a map. 🧩
 *
 * <p>
 * Provides default read-only access to values stored in an internal
 * key–value map. Subclasses typically populate the map during
 * construction.
 * </p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class AbstractDataProvider<K, V> implements DataProvider<K, V> {

    protected final Map<K, V> keyValues;

    /**
     * Creates provider backed by the given map.
     *
     * @param keyValues key–value storage
     */
    protected AbstractDataProvider(Map<K, V> keyValues) {
        this.keyValues = keyValues;
    }

    /**
     * Returns value associated with the given key.
     */
    @Override
    public V getValue(K key) {
        return keyValues.get(key);
    }

    /**
     * Returns unmodifiable map of stored values.
     */
    @Override
    public Map<K, V> getValuesMap() {
        return Collections.unmodifiableMap(keyValues);
    }

    /**
     * Returns set of all stored values.
     */
    @Override
    public Set<V> getValuesSet() {
        return Set.copyOf(keyValues.values());
    }

}