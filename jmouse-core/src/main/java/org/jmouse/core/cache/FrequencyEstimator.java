package org.jmouse.core.cache;

/**
 * 📊 Abstraction over a frequency sketch (e.g., Count–Min Sketch).
 *
 * @param <K> key type
 */
public interface FrequencyEstimator<K> {

    /**
     * Records one observation of the given key.
     */
    void record(KeyWrapper<K> key); // see KeyWrapper note below

    /**
     * Returns an approximate, non-negative frequency estimate for the key.
     */
    int estimate(KeyWrapper<K> key);

    /**
     * Ages or resets the sketch so recent activity dominates.
     */
    void age();

}