package org.jmouse.cad.pcb.physics;

import java.util.Optional;
import java.util.function.IntSupplier;

/** The nearest copper a probe may stand on, within its reach: the node of the nearest offered, made only when it is taken. */
final class NearestCopper {

    private final double reach;
    private double       gap = Double.POSITIVE_INFINITY;
    private IntSupplier  node;

    NearestCopper(double reach) {
        this.reach = reach;
    }

    /** Copper a gap away from the probe, by the node it would stand on. */
    void offer(double offered, IntSupplier offeredNode) {
        if (offered <= reach && (node == null || offered < gap)) {
            gap = Math.max(0, offered);
            node = offeredNode;
        }
    }

    /** The node of the nearest copper offered; empty where none was within reach. */
    Optional<IntSupplier> node() {
        return Optional.ofNullable(node);
    }
}
