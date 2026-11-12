package org.jmouse.mesh.vrml;

import java.util.List;

/** What a VRML97 field holds: numbers, one node, a list of nodes, or words and texts. */
public sealed interface VrmlValue {

    /** {@code 0.8 0.8 0.8}, {@code [0 1 2 -1, 2 3 0 -1]}: every number in order, however grouped. */
    record Numbers(double[] values) implements VrmlValue {
    }

    /** One node, or {@code NULL} as none. */
    record Node(VrmlNode node) implements VrmlValue {
    }

    /** {@code [ Shape { … } Transform { … } ]}. */
    record Nodes(List<VrmlNode> nodes) implements VrmlValue {

        public Nodes {
            nodes = List.copyOf(nodes);
        }
    }

    /** {@code TRUE}, {@code "a text"}, {@code [ "one" "two" ]}: texts without their quotes. */
    record Words(List<String> words) implements VrmlValue {

        public Words {
            words = List.copyOf(words);
        }
    }
}
