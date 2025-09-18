package org.jmouse.core.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🔀 <b>Segmented LRU (SLRU)</b> cache with two segments:
 *
 * <ul>
 *   <li><b>Probation (cold)</b>: newly inserted entries land here; the <i>LRU</i> entry in this segment
 *       is the first candidate for final eviction.</li>
 *   <li><b>Protected (hot)</b>: frequently accessed entries are promoted here; overflow demotes
 *       its LRU entry back to the probation segment.</li>
 * </ul>
 *
 * <h3>Hit/miss & rebalancing rules</h3>
 * <ul>
 *   <li><b>Hit in protected</b>: LRU bump inside the protected segment.</li>
 *   <li><b>Hit in probation</b>: promote to protected (then possibly demote protected’s LRU back to probation if protected overflows).</li>
 *   <li><b>Probation overflow</b>: evict probation’s LRU <i>finally</i> (removed from the cache).</li>
 * </ul>
 *
 * <h3>Why SLRU?</h3>
 * SLRU separates “trial” entries (probation) from “proven hot” entries (protected).
 * This avoids polluting the hot set with one-hit wonders and gives frequent keys longer residency.
 *
 * <h3>Ordering</h3>
 * Both segments use {@link LinkedHashMap} in <b>access-order</b> mode. Iteration order is LRU→MRU,
 * so the first entry in {@code entrySet()} is the segment’s LRU.
 *
 * <h3>Thread-safety</h3>
 * All public methods are {@code synchronized} for coarse-grained safety. For higher concurrency,
 * consider sharding or finer-grained locks.
 *
 * <h3>Complexity</h3>
 * Typical operations are amortized O(1); promotions/demotions perform a bounded number of map ops.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class SegmentLRUCache<K, V> implements BasicCache<K, V> {

    /**
     * Probation (cold) segment, access-order {@link LinkedHashMap}.
     * New entries land here; this segment’s LRU is the first to be evicted.
     */
    private final Map<K, V> probation;

    /**
     * Protected (hot) segment, access-order {@link LinkedHashMap}.
     * Frequently accessed entries are promoted here from probation.
     */
    private final Map<K, V> protectedSegments;

    /**
     * Maximum number of entries allowed in the probation segment.
     */
    private final int probationCapacity;

    /**
     * Maximum number of entries allowed in the protected segment.
     */
    private final int protectedCapacity;

    /**
     * Creates a two-segment SLRU cache with the given capacities.
     *
     * <p>New entries are inserted into the <b>probation</b> segment. On a hit in probation,
     * the entry is promoted to the <b>protected</b> segment. If protected overflows, its LRU
     * is demoted back to probation. If probation overflows, its LRU is evicted (final removal).</p>
     *
     * @param probationCapacity maximum size of the probation (cold) segment
     * @param protectedCapacity maximum size of the protected (hot) segment
     */
    public SegmentLRUCache(int probationCapacity, int protectedCapacity) {
        this.probationCapacity = probationCapacity;
        this.protectedCapacity = protectedCapacity;
        this.probation = new LinkedHashMap<>(16, 0.75f, true);
        this.protectedSegments = new LinkedHashMap<>(16, 0.75f, true);
    }

    /**
     * Returns the <b>oldest (LRU)</b> entry from the given access-ordered map
     * or {@code null} if the map is empty.
     *
     * <p>In {@link LinkedHashMap} with access-order = true, the first iterated entry is LRU.</p>
     */
    public static <K, V> Map.Entry<K, V> oldest(Map<K, V> hashMap) {
        Iterator<Map.Entry<K, V>> iterator = hashMap.entrySet().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Looks up a value by key, applying SLRU promotion rules.
     *
     * <ul>
     *   <li>If found in <b>protected</b> → return (LRU bump within protected).</li>
     *   <li>If found in <b>probation</b> → promote to protected, then rebalance protected
     *       (may demote protected’s LRU back to probation), then return.</li>
     *   <li>If absent → return {@code null}.</li>
     * </ul>
     *
     * @param key lookup key
     * @return the value if present, otherwise {@code null}
     */
    public synchronized V get(K key) {
        V value = protectedSegments.get(key);

        if (value != null) {
            return value;
        }

        value = probation.get(key);

        if (value != null) {
            probation.remove(key);
            protectedSegments.put(key, value);
            reBalanceProtected();
        }

        return value;
    }

    /**
     * Inserts or updates an entry.
     *
     * <ul>
     *   <li>If the key exists in <b>protected</b> → update in-place (LRU bump).</li>
     *   <li>Otherwise → insert into <b>probation</b> and re-balance probation
     *       (evict probation’s LRU on overflow).</li>
     * </ul>
     *
     * @param key   entry key
     * @param value entry value
     */
    public synchronized void set(K key, V value) {
        if (protectedSegments.containsKey(key)) {
            protectedSegments.put(key, value);
            return;
        }

        probation.put(key, value);

        reBalanceProbation();
    }

    /**
     * 🗑️ Remove value by key (return previous or null).
     *
     * @param key
     */
    @Override
    public V remove(K key) {
        V previous = protectedSegments.remove(key);

        if (previous != null) {
            return previous;
        }

        return probation.remove(key);
    }

    /**
     * Rebalances the protected (hot) segment by demoting its LRU back to probation
     * until the protected segment fits within {@link #protectedCapacity}.
     *
     * <p>Each demotion may trigger a probation rebalance (which may evict probation’s LRU).</p>
     */
    private void reBalanceProtected() {
        while (protectedSegments.size() > protectedCapacity) {
            Map.Entry<K, V> oldest = oldest(protectedSegments);
            if (oldest == null) {
                break;
            }

            protectedSegments.remove(oldest.getKey());
            probation.put(oldest.getKey(), oldest.getValue());

            reBalanceProbation();
        }
    }

    /**
     * Rebalances the probation (cold) segment by evicting its LRU entry
     * until the segment fits within {@link #probationCapacity}.
     */
    private void reBalanceProbation() {
        while (probation.size() > probationCapacity) {
            Map.Entry<K, V> oldest = oldest(probation);

            if (oldest == null) {
                break;
            }

            probation.remove(oldest.getKey());
        }
    }

    /**
     * @return total number of entries across both segments
     */
    public synchronized int size() {
        return probation.size() + protectedSegments.size();
    }

    /**
     * 🧹 Clear all entries.
     */
    @Override
    public void clear() {
        probation.clear();
        protectedSegments.clear();
    }

    /**
     * @return direct view of the probation (cold) segment map (access-order)
     */
    public synchronized Map<K, V> getProbation() {
        return probation;
    }

    /**
     * @return direct view of the protected (hot) segment map (access-order)
     */
    public synchronized Map<K, V> getProtected() {
        return protectedSegments;
    }

    /**
     * @return current number of entries in the probation segment
     */
    public synchronized int getProbationSize() {
        return probation.size();
    }

    /**
     * Indicates whether the probation segment is at or beyond its capacity.
     *
     * <p>Note the comparison is {@code >=} (not {@code >}). This is useful for admission
     * policies that want to <i>pre-check</i> and potentially evict/compare before inserting a new entry.</p>
     *
     * @return {@code true} if {@code probation.size() >= probationCapacity}, else {@code false}
     */
    public synchronized boolean isProbationExceed() {
        return getProbationSize() >= getProbationCapacity();
    }

    /**
     * @return current number of entries in the protected segment
     */
    public synchronized int getProtectedSize() {
        return protectedSegments.size();
    }

    /**
     * @return configured capacity of the probation segment
     */
    public synchronized int getProbationCapacity() {
        return probationCapacity;
    }

    /**
     * @return configured capacity of the protected segment
     */
    public synchronized int getProtectedCapacity() {
        return protectedCapacity;
    }
}
