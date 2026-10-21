package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;

import java.util.List;

/**
 * A circuit's nodes and sources as this library's engine numbers them — what another engine names its columns by, so
 * its answer reads as this one's: a node's voltage by the node, a source's current by the source.
 *
 * @param nodes   every node but ground, as the engine's datasets name them; a part's inner nodes among them
 * @param sources every source and generator, by part id, in the circuit's order
 */
public record CircuitTopology(List<String> nodes, List<String> sources) {

    /**
     * The resistance behind the engine's gmin: every analog node a model touches leaks to ground through it. Another
     * engine gives a pin the circuit connects to nothing the same leak, so that node is what the engine solves.
     */
    public static final double NODE_LEAK_RESISTANCE = Netlist.LEAK_RESISTANCE;

    public CircuitTopology {
        nodes = List.copyOf(nodes);
        sources = List.copyOf(sources);
    }

    public static CircuitTopology of(CompiledObject circuit) {
        Netlist netlist = Netlist.of(circuit);
        List<String> sources = netlist.elements().stream().filter(element -> element.kind().isSource())
                .map(Netlist.Element::id).toList();
        return new CircuitTopology(netlist.nodes(), sources);
    }
}
