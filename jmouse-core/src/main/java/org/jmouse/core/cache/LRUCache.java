package org.jmouse.core.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ♻️ <b>LRUCache</b> — a thread-safe, bounded-size cache with <i>Least Recently Used</i> eviction policy.
 *
 * <p>Implemented as an {@link LinkedHashMap} in access-order mode, with
 * eviction performed automatically when the maximum size is exceeded.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>O(1) get/put operations.</li>
 *   <li>Automatic eviction of the eldest entry when {@code maxSize} is exceeded.</li>
 *   <li>Thread-safe by guarding all operations with a {@link ReentrantLock}.</li>
 *   <li>Compact and predictable baseline; for high concurrency workloads, a segmented design may be preferable.</li>
 * </ul>
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public class LRUCache<K, V> implements BasicCache<K, V> {

    /**
     * Default load factor for the internal {@link LinkedHashMap}.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private final int maximumSize;

    /**
     * Lock to provide thread-safety for all operations.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Delegate map maintaining access order and eviction policy.
     */
    private final LinkedHashMap<K, V> delegate;

    /**
     * Creates a new {@code LRUCache} with the given maximum size.
     *
     * @param maximumSize the maximum number of entries before eviction occurs
     */
    public LRUCache(int maximumSize) {
        this.maximumSize = maximumSize;
        this.delegate = new LinkedHashMap<>(maximumSize * 2, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maximumSize;
            }
        };
    }

    /**
     * Returns the value associated with the given key, or {@code null} if absent.
     *
     * @param key the key to look up
     * @return the value if present, otherwise {@code null}
     */
    public V get(K key) {
        lock.lock();
        try {
            return delegate.get(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Associates the given value with the given key in the cache.
     * Evicts the least recently used entry if size exceeds {@code maximumSize}.
     *
     * @param key   the key
     * @param value the value
     */
    public void set(K key, V value) {
        lock.lock();
        try {
            delegate.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the entry for the given key if present.
     *
     * @param key the key to remove
     * @return the previous value, or {@code null} if none
     */
    public V remove(K key) {
        lock.lock();
        try {
            return delegate.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of entries currently in the cache.
     *
     * @return live entry count
     */
    public int size() {
        lock.lock();
        try {
            return delegate.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        lock.lock();
        try {
            delegate.clear();
        } finally {
            lock.unlock();
        }
    }
}
