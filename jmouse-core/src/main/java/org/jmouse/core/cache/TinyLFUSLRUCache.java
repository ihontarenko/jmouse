package org.jmouse.core.cache;

import java.util.Map;

/**
 * 🔀 <b>TinyLFU-over-SLRU</b> cache.
 *
 * <p>Combines:</p>
 * <ul>
 *   <li><b>SLRU</b> (Segmented LRU): two segments — <i>probation</i> (cold, new/rehydrated) and
 *       <i>protected</i> (hot, promoted on hits).</li>
 *   <li><b>TinyLFU</b> admission: a <i>Doorkeeper</i> (two-bit front filter) + <i>Count–Min Sketch</i> (CMS)
 *       track approximate frequencies. On probation overflow, a candidate is admitted <b>only if</b> its
 *       estimated freq strictly exceeds the probation LRU victim.</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>📊 <b>touch(key)</b>: updates TinyLFU stats.
 *       First sighting → Doorkeeper sets 2 hashed bits (cheap, no CMS write).
 *       Repeat sighting → passes Doorkeeper → counted in CMS across rows.</li>
 *   <li>📚 <b>get(key)</b>: SLRU lookup. Protected hit → LRU bump.
 *       Probation hit → promote to protected (may demote a protected tail back to probation).</li>
 *   <li>📦 <b>put(key,value)</b>: if probation would overflow,
 *       compare {@code estimate(candidate)} vs {@code estimate(victim)} and admit only when <i>candidate &gt; victim</i>.</li>
 *   <li>🧽 <b>maintenance()</b>: periodically decay CMS (recentness bias) and clear Doorkeeper (require two fresh sightings again).</li>
 * </ol>
 *
 * <h3>Why two Doorkeeper bits?</h3>
 * <ul>
 *   <li>🧱 Requires ≥2 sightings before CMS increments (reduces one-hit wonders/pollution).</li>
 *   <li>🪄 Two independent hashes lower false positives vs a single bit at comparable space.</li>
 *   <li>⚡ Ultra-cheap front gate; CMS work happens only after passing the gate.</li>
 * </ul>
 *
 * <h3>Complexity</h3>
 * <ul>
 *   <li>get/put: amortized O(1)</li>
 *   <li>admission compare: O(1) (two CMS lookups)</li>
 *   <li>maintenance: O(depth×width) for CMS decay; O(1) for Doorkeeper reset</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * Coarse-grained via {@code synchronized} on public methods. For higher throughput, consider sharding or finer-grained locks.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TinyLFUSLRUCache<K, V> implements BasicCache<K, V> {

    /**
     * 🧊 Two-segment LRU container (probation + protected).
     */
    private final SegmentLRUCache<K, V> internalCache;

    /**
     * 🧠 TinyLFU core (Doorkeeper + CMS + aging).
     */
    private final TinyLFU<K> tinyLFU;

    /**
     * ✨ Create a TinyLFU-over-SLRU cache.
     *
     * <p><b>Segments:</b> {@code probationCapacity} bounds the cold segment; {@code protectedCapacity} bounds the hot segment.
     * New entries start in probation; hits promote to protected.</p>
     *
     * <p><b>Admission (TinyLFU):</b> {@code sketchWidth} hints CMS width (rounded internally).
     * Doorkeeper is sized as {@code sketchWidth * 8} to keep FP low at the front gate.</p>
     *
     * @param probationCapacity  max size of the probation (cold) segment
     * @param protectedCapacity  max size of the protected (hot) segment
     * @param sketchWidth        width hint for the Count–Min Sketch
     */
    public TinyLFUSLRUCache(int probationCapacity, int protectedCapacity, int sketchWidth) {
        Doorkeeper<K>     keeper = new Doorkeeper<>(sketchWidth * 8);
        CountMinSketch<K> sketch = new CountMinSketch<>(sketchWidth);

        this.tinyLFU = new TinyLFU<>(
                new DoorkeeperSeenFilter<>(keeper),
                new CmsFrequencyEstimator<>(sketch)
        );
        this.internalCache = new SegmentLRUCache<>(probationCapacity, protectedCapacity);
    }

    /**
     * 🔎 Peek (no remove) the current LRU victim from probation (for admission decisions).
     */
    private Map.Entry<K, V> peekProbationVictim() {
        return SegmentLRUCache.oldest(internalCache.getProbation());
    }

    /**
     * 📥 Get a value (if present), update TinyLFU stats, and perform SLRU promotions as needed.
     *
     * @param key lookup key
     * @return cached value or {@code null} if absent
     */
    public synchronized V get(K key) {
        touch(key);
        return internalCache.get(key);
    }

    /**
     * 📦 Insert or update with TinyLFU admission on probation overflow.
     * Rejects candidates that are not strictly more frequent than the probation victim (prevents pollution).
     *
     * @param key   entry key
     * @param value entry value
     */
    public synchronized void set(K key, V value) {
        touch(key);

        // Fast path: already present (get() above may have promoted it).
        if (internalCache.get(key) != null) {
            internalCache.set(key, value);
            return;
        }

        Map<K, V>       probation = internalCache.getProbation();
        Map.Entry<K, V> victim    = peekProbationVictim();

        // Admission check only when probation would exceed capacity,
        // and the candidate is not already present in probation.
        if (internalCache.isProbationExceed() && victim != null && !probation.containsKey(key)) {
            if (admit(key, victim.getKey())) {
                // Candidate wins: remove current probation LRU victim, then insert candidate.
                probation.remove(victim.getKey());
            } else {
                // Candidate loses (<= victim): reject insertion to prevent cache pollution.
                return;
            }
        }

        internalCache.set(key, value);
    }

    /**
     * 🗑️ Remove value by key (return previous or null).
     *
     * @param key
     */
    @Override
    public V remove(K key) {
        return internalCache.remove(key);
    }

    /**
     * 🔢 Live entry count (may perform lazy cleanup).
     */
    @Override
    public int size() {
        return internalCache.size();
    }

    /**
     * 🧹 Clear all entries.
     */
    @Override
    public void clear() {
        internalCache.clear();
    }

    /**
     * 🧽 Periodic upkeep: decay CMS (bias to recent traffic) and clear Doorkeeper
     * (require two fresh sightings before CMS increments again).
     */
    public synchronized void maintenance() {
        tinyLFU.maintenance();
    }

    /**
     * 🫖 Record an access in TinyLFU:
     * <ul>
     *   <li>First seen → set two Doorkeeper bits, no CMS write.</li>
     *   <li>Seen again → passes Doorkeeper → increment CMS rows.</li>
     * </ul>
     *
     * @param key accessed key
     */
    private void touch(K key) {
        tinyLFU.recordAccess(key);
    }

    /**
     * ⚖️ Admission rule: admit when {@code estimate(candidate) > estimate(victim)}.
     * Strict greater-than avoids churn (“trading equals”).
     *
     * @param candidate key to insert
     * @param victim    current probation LRU victim
     * @return {@code true} to evict victim and admit candidate; {@code false} otherwise
     */
    public boolean admit(K candidate, K victim) {
        return tinyLFU.shouldAdmit(candidate, victim);
    }
}
