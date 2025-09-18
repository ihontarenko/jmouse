package org.jmouse.core.cache;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🕒 <b>TTLCache</b> — a thread-safe cache with per-entry time-to-live (TTL) expiration.
 *
 * <p>This cache automatically evicts expired entries lazily, during read/write
 * operations such as {@link #get(Object)}, {@link #size()}, {@link #entrySet()}, etc.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Implements the {@link Map} interface for drop-in compatibility.</li>
 *   <li>Entries are stored with a timestamp and removed once the TTL is exceeded.</li>
 *   <li>Uses {@link ConcurrentHashMap} as the underlying delegate for thread-safety.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>All time measurements use {@link System#nanoTime()} for monotonic precision.</li>
 *   <li>Methods like {@link #size()} and {@link #values()} perform cleanup,
 *       which may impact performance on large caches.</li>
 * </ul>
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
@SuppressWarnings("NullableProblems")
public class TTLCache<K, V> implements Map<K, V>, BasicCache<K, V> {

    /**
     * Time-to-live in nanoseconds.
     */
    private final long ttl;

    /**
     * Delegate map storing value holders with timestamps.
     */
    private final Map<K, Holder<V>> delegate = new ConcurrentHashMap<>();

    /**
     * Creates a new cache with the given TTL in milliseconds.
     *
     * @param timeToLiveMillis entry lifetime in milliseconds
     */
    public TTLCache(long timeToLiveMillis) {
        this.ttl = timeToLiveMillis * 1_000_000L;
    }

    /**
     * Returns the value for the given key if present and not expired.
     * Expired entries are removed lazily.
     */
    @Override
    public V get(Object key) {
        Holder<V> entry = delegate.get(key);

        if (entry == null || isExpired(entry)) {
            delegate.remove(key, entry);
            return null;
        }

        return entry.value();
    }

    @Override
    public V put(K key, V value) {
        Holder<V> previous = delegate.put(key, new Holder<>(value, System.nanoTime()));
        return previous == null || isExpired(previous) ? null : previous.value();
    }

    /**
     * Sets a value into the cache, replacing any existing entry.
     */
    @Override
    public void set(K key, V value) {
        put(key, value);
    }

    /**
     * Returns the number of live entries in the cache.
     * Performs cleanup of expired entries before counting.
     */
    @Override
    public int size() {
        cleanup();
        return delegate.size();
    }

    /**
     * Returns {@code true} if the cache contains no live entries.
     * Performs cleanup before the check.
     */
    @Override
    public boolean isEmpty() {
        cleanup();
        return delegate.isEmpty();
    }

    /**
     * Returns {@code true} if the given key maps to a live (not expired) entry.
     */
    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Returns {@code true} if any live entry maps to the given value.
     * Performs cleanup before the check.
     */
    @Override
    public boolean containsValue(Object value) {
        cleanup();
        return delegate.values().stream()
                .filter(holder -> !isExpired(holder))
                .anyMatch(holder -> Objects.equals(holder.value(), value));
    }

    /**
     * Removes the entry for the given key.
     *
     * @return the previous value if present and not expired, otherwise {@code null}
     */
    @Override
    public V remove(Object key) {
        Holder<V> removed = delegate.remove(key);
        return removed == null || isExpired(removed) ? null : removed.value();
    }

    /**
     * Bulk put operation. Entries are inserted with the current timestamp.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::set);
    }

    /**
     * Clears the entire cache.
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * Returns the set of live keys. Performs cleanup before returning.
     */
    @Override
    public Set<K> keySet() {
        cleanup();
        return delegate.keySet();
    }

    /**
     * Returns a collection of live values. Performs cleanup before returning.
     */
    @Override
    public Collection<V> values() {
        cleanup();

        List<V> collection = new ArrayList<>();
        for (Holder<V> holder : delegate.values()) {
            if (!isExpired(holder)) {
                collection.add(holder.value());
            }
        }

        return collection;
    }

    /**
     * Returns a set of live entries as {@link SimpleEntry} objects.
     * Performs cleanup before returning.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        cleanup();

        Set<Entry<K, V>> entrySet = new HashSet<>();
        for (Map.Entry<K, Holder<V>> entry : delegate.entrySet()) {
            if (!isExpired(entry.getValue())) {
                entrySet.add(new SimpleEntry<>(entry.getKey(), entry.getValue().value()));
            }
        }

        return entrySet;
    }

    /**
     * Removes expired entries from the delegate map.
     */
    private void cleanup() {
        delegate.entrySet().removeIf(this::isExpired);
    }

    /**
     * Checks if a holder has exceeded its TTL.
     */
    private boolean isExpired(Entry<K, Holder<V>> entry) {
        return isExpired(entry.getValue());
    }

    /**
     * Checks if a holder has exceeded its TTL.
     */
    private boolean isExpired(Holder<V> holder) {
        return (System.nanoTime() - holder.timestamp) > ttl;
    }

    /**
     * Internal value holder with timestamp.
     */
    private record Holder<V>(V value, long timestamp) {
    }
}
