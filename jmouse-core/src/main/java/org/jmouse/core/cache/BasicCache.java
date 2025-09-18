package org.jmouse.core.cache;

/**
 * 🧰 Minimal cache API: get/put/remove/size/clear.
 * <p>Designed to be easy to implement over LRU, TTL, SLRU, TinyLFU-over-SLRU, etc.</p>
 */
public interface BasicCache<K, V> {

    /**
     * 🔎 Get value by key (null if absent/expired).
     */
    V get(K key);

    /**
     * 💾 Put/replace value by key.
     */
    void set(K key, V value);

    /**
     * 🗑️ Remove value by key (return previous or null).
     */
    V remove(K key);

    /**
     * 🔢 Live entry count (may perform lazy cleanup).
     */
    int size();

    /**
     * 🧹 Clear all entries.
     */
    void clear();

    /**
     * ❓ Convenience: whether a live value is mapped for the key.
     */
    default boolean containsKey(K key) {
        return get(key) != null;
    }
}