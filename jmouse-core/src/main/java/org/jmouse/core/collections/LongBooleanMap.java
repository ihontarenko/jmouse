package org.jmouse.core.collections;

import java.util.BitSet;
import java.util.function.LongPredicate;

/**
 * 🗂️ A map from a primitive {@code long} to a yes or a no, neither boxed: what a search has already measured — a step clear, a
 * place a via fits — asked again many times. Not safe to share between threads.
 */
public final class LongBooleanMap {

    private LongSlots slots;
    private BitSet    values;

    public LongBooleanMap() {
        this(16);
    }

    /** @param expected how many keys it is expected to hold: it grows past them, but starts large enough for them */
    public LongBooleanMap(int expected) {
        slots = new LongSlots(expected);
        values = new BitSet(slots.capacity());
    }

    /** Whether a value is kept for a key. */
    public boolean containsKey(long key) {
        return slots.slotOf(key) >= 0;
    }

    /** The value kept for a key, or {@code missing} where none is. */
    public boolean get(long key, boolean missing) {
        int slot = slots.slotOf(key);
        return slot >= 0 ? values.get(slot) : missing;
    }

    /** Keeps a value for a key, in place of any kept before. */
    public void put(long key, boolean value) {
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            values.set(slot, value);
            return;
        }
        int claimed = claim(key, slot);
        values.set(claimed, value);
    }

    /** The value kept for a key — worked out and kept the first time it is asked. */
    public boolean computeIfAbsent(long key, LongPredicate worked) {
        int slot = slots.slotOf(key);
        if (slot >= 0) {
            return values.get(slot);
        }
        boolean value = worked.test(key);
        // The work may not touch this map, so the free slot it was given still stands; the slot is claimed before the values are
        // named, since claiming may grow them into new ones.
        int claimed = claim(key, slot);
        values.set(claimed, value);
        return value;
    }

    /** How many keys it holds. */
    public int size() {
        return slots.size();
    }

    private int claim(long key, int missing) {
        if (slots.full()) {
            LongSlots.Doubled doubled = slots.doubled();
            BitSet larger = new BitSet(doubled.slots().capacity());
            int[] moved = doubled.moved();
            for (int slot = values.nextSetBit(0); slot >= 0; slot = values.nextSetBit(slot + 1)) {
                larger.set(moved[slot]);
            }
            slots = doubled.slots();
            values = larger;
            missing = slots.slotOf(key);
        }
        return slots.claim(key, missing);
    }
}
