package org.jmouse.parallax.circuit;

import org.jmouse.parallax.numerics.SparseSystem;

/**
 * Where a two-terminal device's conductance goes in the matrix — its four entries' slots, found once and kept, as a
 * SPICE device keeps pointers into its matrix: a capacitor stamped at every step looked its entries up every time. The
 * slots belong to one system; asked by another, they are found again.
 */
final class ConductanceSlots {

    private SparseSystem system;
    /** positive-positive, negative-negative, positive-negative, negative-positive; -1 where a row or column is ground. */
    private final int[]  slots = new int[4];

    int[] resolve(SparseSystem asking, Assembly assembly, int positive, int negative) {
        if (system != asking) {
            slots[0] = assembly.slot(positive, positive);
            slots[1] = assembly.slot(negative, negative);
            slots[2] = assembly.slot(positive, negative);
            slots[3] = assembly.slot(negative, positive);
            system = asking;
        }
        return slots;
    }
}
