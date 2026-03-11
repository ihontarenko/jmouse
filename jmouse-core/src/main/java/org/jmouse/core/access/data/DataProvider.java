package org.jmouse.core.access.data;

import java.util.Map;
import java.util.Set;

/**
 * Generic provider for key–value data access. 📦
 *
 * <p>
 * Exposes read access to individual values as well as the
 * underlying map and value set representations.
 * </p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface DataProvider<K, V> {

    /**
     * Returns value associated with the given key.
     *
     * @param key lookup key
     *
     * @return value or {@code null} if not present
     */
    V getValue(K key);

    /**
     * Returns all values as a map.
     *
     * @return key–value map
     */
    Map<K, V> getValuesMap();

    /**
     * Returns all values as a set.
     *
     * @return value set
     */
    Set<V> getValuesSet();

}