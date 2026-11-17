package org.jmouse.core.collections;

/**
 * 🔑 The slots of a table keyed by a primitive {@code long}: open addressing, the key's hash mixed so neighbouring keys — a grid's
 * cells, packed column and row — spread over the table rather than crowd one run. Shared by every such table of this package; each
 * keeps its own values beside these slots, at the same places.
 */
final class LongSlots {

    /** How full a table grows before it doubles: past it, a probe walks too far. */
    private static final double MOST_FULL = 0.6;

    private final long[]    keys;
    private final boolean[] taken;
    private int             size;

    LongSlots(int expected) {
        int capacity = Integer.highestOneBit(Math.max(4, (int) Math.ceil(expected / MOST_FULL)) - 1) << 1;
        keys = new long[capacity];
        taken = new boolean[capacity];
    }

    /** How many places the table has: the length of the values kept beside it. */
    int capacity() {
        return keys.length;
    }

    int size() {
        return size;
    }

    /** The slot a key stands in, or — where it is not there — the slot it would take: {@code -(slot + 1)}. */
    int slotOf(long key) {
        int mask = keys.length - 1;
        int slot = mixed(key) & mask;
        while (taken[slot]) {
            if (keys[slot] == key) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
        return -(slot + 1);
    }

    /** Whether one more key would fill the table past its measure: the holder doubles it first ({@link #doubled}). */
    boolean full() {
        return size + 1 > keys.length * MOST_FULL;
    }

    /** Takes the free slot {@link #slotOf} named for a key that is not there, and answers it. */
    int claim(long key, int missing) {
        int slot = -missing - 1;
        keys[slot] = key;
        taken[slot] = true;
        size++;
        return slot;
    }

    /**
     * Slots twice as many holding the same keys, and where each moved: {@code moved[old]} is the new slot of the key at {@code old},
     * {@code -1} where none stood. The holder moves its values by it.
     */
    Doubled doubled() {
        LongSlots larger = new LongSlots(keys.length);
        int[] moved = new int[keys.length];
        for (int slot = 0; slot < keys.length; slot++) {
            moved[slot] = taken[slot] ? larger.claim(keys[slot], larger.slotOf(keys[slot])) : -1;
        }
        return new Doubled(larger, moved);
    }

    record Doubled(LongSlots slots, int[] moved) {
    }

    /** A key's hash, its bits mixed (the finaliser of MurmurHash3). */
    private static int mixed(long key) {
        long mixed = key;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (int) mixed;
    }
}
