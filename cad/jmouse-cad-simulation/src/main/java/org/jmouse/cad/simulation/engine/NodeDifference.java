package org.jmouse.cad.simulation.engine;

/**
 * A difference of two nodes a probe reads across a part: the positive node less the negative one, each an index into
 * the nodes it is numbered among, {@link #GROUND} for ground.
 */
public record NodeDifference(int positive, int negative) {

    /** Ground's index: it reads 0. */
    public static final int GROUND = -1;
}
