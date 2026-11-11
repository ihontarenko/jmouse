package org.jmouse.cad.spice;

import org.jmouse.spice.node.SpiceStatement;

import java.util.List;
import java.util.Map;

/**
 * What one SPICE program needs beside the netlist every program reads alike: how it is told to run a transient and print
 * every vector, and how what it printed reads back. ngspice takes a {@code .control} block and prints tables; Xyce takes
 * {@code .options} and a {@code .print … format=csv}. The deck itself — the circuit — is written once for all of them.
 */
public interface SpiceDialect {

    /** The program that speaks it, as a message says it: {@code ngspice}, {@code Xyce}. */
    String program();

    /**
     * What closes the deck, before its {@code .end}: the run at {@code celsius}, printed every {@code step} seconds, every
     * node and every branch.
     */
    List<SpiceStatement> runAndPrint(double step, double celsius);

    /**
     * What the program printed, as vectors under SPICE's own names in lower case.
     *
     * @throws SpiceRunException when it printed no run
     */
    PrintedVectors read(String output);

    /**
     * A printed transient.
     *
     * @param nodes    each node's voltage, by the node's name: {@code out}
     * @param branches each branch's current, by its element's name: {@code v1}
     */
    record PrintedVectors(double[] time, Map<String, double[]> nodes, Map<String, double[]> branches) {

        public PrintedVectors {
            nodes = Map.copyOf(nodes);
            branches = Map.copyOf(branches);
        }
    }
}
