package org.jmouse.core.cache;

/**
 * 🚪 "Seen-before" filter that suppresses one-hit noise.
 *
 * @param <K> key type
 */
public interface SeenFilter<K> {

    /**
     * @return true if the key is considered "seen before" (passes through),
     * false if this is the first sighting (bits set, but blocked)
     */
    boolean pass(KeyWrapper<K> key);

    /**
     * Clears all internal marks.
     */
    void reset();

}