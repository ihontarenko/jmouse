package org.jmouse.parallax.circuit;

import java.util.List;
import java.util.Set;

/**
 * Where a node a probe names stands among a sample's voltages: ground, which reads 0; a node of the circuit, at its
 * index; or a node the circuit does not have, which reads nothing (NaN). Resolved once per frame, read on every sample.
 */
sealed interface NodeReference {

    /** Names ground has besides the netlist's own {@code 0}. */
    Set<String> GROUND_NAMES = Set.of("0", "ground", "GND");

    record Ground() implements NodeReference {
        @Override
        public double in(double[] voltages) {
            return 0;
        }
    }

    record Absent() implements NodeReference {
        @Override
        public double in(double[] voltages) {
            return Double.NaN;
        }
    }

    record Node(int index) implements NodeReference {
        @Override
        public double in(double[] voltages) {
            return voltages[index];
        }
    }

    NodeReference GROUND = new Ground();
    NodeReference ABSENT = new Absent();

    /** Its value among these per-node values — a voltage, a peak, a mean. */
    double in(double[] voltages);

    /** A node's place among {@code order}. */
    static NodeReference of(List<String> order, String node) {
        if (GROUND_NAMES.contains(node)) {
            return GROUND;
        }
        int index = order.indexOf(node);
        return index < 0 ? ABSENT : new Node(index);
    }

    /** As the solver's node differences number it: −1 for ground; null for a node the circuit does not have. */
    default Integer solverIndex() {
        return switch (this) {
            case Ground ground -> -1;
            case Absent absent -> null;
            case Node node -> node.index();
        };
    }
}
