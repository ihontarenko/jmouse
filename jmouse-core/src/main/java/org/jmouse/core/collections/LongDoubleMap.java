package org.jmouse.core.collections;

import java.util.function.LongToDoubleFunction;

/**
 * 🗂️ A map from a primitive {@code long} to a primitive {@code double}, neither boxed: what a search remembers per point of its grid,
 * asked millions of times, where a {@code Map<Long, Double>} spent its time making and hashing objects. Not safe to share between
 * threads.
 */
public final class LongDoubleMap {

    private LongSlots slots;
    private double[]  values;

    public LongDoubleMap() {
        this(16);
    }

    /** @param expected how many keys it is expected to hold: it grows past them, but starts large enough for them */
    public LongDoubleMap(int expected) {
        slots = new LongSlots(expected);
        values = new double[slots.capacity()];
    }

    /** The value kept for a key, or {@code missing} where none is. */
    public double get(long key, double missing) {
        int slot = slots.slotOf(key);
        return slot >= 0 ? values[slot] : missing;
    }

    /** Whether a value is kept for a key. */
    public boolean containsKey(long key) {
        return slots.slotOf(key) >= 0;
    }

    /** Keeps a value for a key, in place of any kept before. */
    public void put(long key, double value) {
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            values[slot] = value;
            return;
        }
        int claimed = claim(key, slot);
        values[claimed] = value;
    }

    /** The value kept for a key — worked out and kept the first time it is asked. */
    public double computeIfAbsent(long key, LongToDoubleFunction worked) {
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            return values[slot];
        }
        double value = worked.applyAsDouble(key);
        // The work may not touch this map, so the free slot it was given still stands; the slot is claimed before the values are
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
            double[] larger = new double[doubled.slots().capacity()];
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
