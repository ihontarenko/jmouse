package org.jmouse.core.collections;

import java.util.function.LongFunction;

/**
 * 🗂️ A map from a primitive {@code long} to an object, the key unboxed and its hash mixed: the cells of a grid index — column and
 * row packed into one key —, which a {@code HashMap<Long, …>} crowds into the same buckets along every diagonal, since
 * {@link Long#hashCode()} folds the two halves together. Not safe to share between threads.
 *
 * @param <V> what is kept per key; never null
 */
public final class LongObjectMap<V> {

    private LongSlots slots;
    private Object[]  values;

    public LongObjectMap() {
        this(16);
    }

    /** @param expected how many keys it is expected to hold: it grows past them, but starts large enough for them */
    public LongObjectMap(int expected) {
        slots = new LongSlots(expected);
        values = new Object[slots.capacity()];
    }

    /** The value kept for a key, or null where none is. */
    @SuppressWarnings("unchecked")
    public V get(long key) {
        int slot = slots.slotOf(key);
        return slot >= 0 ? (V) values[slot] : null;
    }

    /** The value kept for a key, or {@code missing} where none is. */
    public V getOrDefault(long key, V missing) {
        V value = get(key);
        return value != null ? value : missing;
    }

    /** Keeps a value for a key, in place of any kept before. */
    public void put(long key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("A long map keeps no null: key " + key);
        }
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            values[slot] = value;
            return;
        }
        int claimed = claim(key, slot);
        values[claimed] = value;
    }

    /** The value kept for a key — made and kept the first time it is asked. */
    @SuppressWarnings("unchecked")
    public V computeIfAbsent(long key, LongFunction<? extends V> made) {
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            return (V) values[slot];
        }
        V value = made.apply(key);
        if (value == null) {
            throw new IllegalArgumentException("A long map keeps no null: key " + key);
        }
        // The making may not touch this map, so the free slot it was given still stands; the slot is claimed before the values are
        // named, since claiming may grow them into new ones.
        int claimed = claim(key, slot);
        values[claimed] = value;
        return value;
    }

    /** How many keys it holds. */
    public int size() {
        return slots.size();
    }

    private int claim(long key, int missing) {
        if (slots.full()) {
            LongSlots.Doubled doubled = slots.doubled();
            Object[] larger = new Object[doubled.slots().capacity()];
            int[] moved = doubled.moved();
            for (int slot = 0; slot < moved.length; slot++) {
                if (moved[slot] >= 0) {
                    larger[moved[slot]] = values[slot];
                }
            }
            slots = doubled.slots();
            values = larger;
            missing = slots.slotOf(key);
        }
        return slots.claim(key, missing);
    }
}
