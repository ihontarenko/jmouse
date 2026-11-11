package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the engine answers of an elaborated circuit: the one door every analysis is asked through, so a reader never
 * holds the solver's numbering — every answer is by the circuit's own names.
 */
public final class CircuitSimulation {

    private CircuitSimulation() {
    }

    /**
     * The circuit at rest — capacitors open, inductors shorted, every source at its constant part —: each analog net's
     * voltage, by its name, then each current a generator carries, by the generator's reference, then each continuous
     * state, by its unit and name.
     *
     * @throws IllegalArgumentException when it has no unique solution, or does not settle
     */
    public static Map<String, Double> operatingPoint(ElaboratedCircuit circuit) {
        Netlist netlist = Netlist.of(circuit);
        double[] solution = new CircuitSolver(netlist).operatingPoint();
        Map<String, Double> named = new LinkedHashMap<>();
        for (int node = 0; node < netlist.nodes().size(); node++) {
            named.put(netlist.nodes().get(node), solution[node]);
        }
        for (int branch = 0; branch < netlist.branches().size(); branch++) {
            if (netlist.branches().get(branch).partCurrent()) {
                named.put(netlist.branches().get(branch).name(), solution[netlist.nodes().size() + branch]);
            }
        }
        int stateBase = netlist.nodes().size() + netlist.branches().size();
        for (int state = 0; state < netlist.states().size(); state++) {
            named.put(netlist.states().get(state), solution[stateBase + state]);
        }
        return named;
    }
}
