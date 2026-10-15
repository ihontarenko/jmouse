package org.jmouse.parallax.circuit;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A circuit's transient worked out at once — what a computed graph, an object page and a datasheet check read —, by the
 * one engine the live bench runs ({@link LiveCircuitSolver}, keeping no screen readings): SPICE's step control, the
 * cut into pieces, models compiled to code. Samples are evenly spaced from 0 to the circuit's duration, both ends
 * included; each is read off the solution, never forcing a step to land on it.
 *
 * <p>What it holds is named as the circuit names it: a node's voltage by the node, a part's current by
 * {@link CircuitComputations#partCurrentColumn} ({@code V1}, {@code D1}, {@code U1.output}), a digital node's level by
 * the node.</p>
 */
public final class TransientRun {

    /** Samples are asked of the engine this many at a time, as it allows. */
    private static final int CHUNK = 256;

    private final double[]                  times;
    private final Map<String, double[]>     voltages;
    private final Map<String, double[]>     currents;
    private final List<Map<String, Object>> levels;

    private TransientRun(double[] times, Map<String, double[]> voltages, Map<String, double[]> currents,
                         List<Map<String, Object>> levels) {
        this.times = times;
        this.voltages = voltages;
        this.currents = currents;
        this.levels = levels;
    }

    /**
     * Runs the circuit over its duration, with the currents of the parts that carry one of their own — sources,
     * generators, inductors, diodes —, or of every part.
     *
     * @param samples   how many evenly spaced samples, at least two
     * @param everyPart every part's current, for a sheet that draws them all; tracing four hundred parts at every
     *                  sample cost more than solving them
     */
    public static TransientRun of(CompiledObject object, int samples, boolean everyPart, ComputationProgress progress) {
        if (samples < 2) {
            throw new IllegalArgumentException("A transient has at least two samples.");
        }
        Netlist netlist = Netlist.of(object);
        double duration = netlist.duration();
        LiveCircuitSolver solver = new LiveCircuitSolver(object, duration / (samples - 1)).solutionOnly();
        solver.trace(1);
        solver.traceOnly(everyPart ? null : partsWithCurrentsOfTheirOwn(netlist));
        List<String> nodes = solver.nodes();
        double[] times = new double[samples];
        Map<String, double[]> voltages = new LinkedHashMap<>();
        nodes.forEach(node -> voltages.put(node, new double[samples]));
        Map<String, double[]> currents = new LinkedHashMap<>();
        List<Map<String, Object>> levels = new ArrayList<>(samples);
        boolean digital = !solver.levels().isEmpty();
        int done = 0;
        while (done < samples) {
            // A digital node's level is the engine's state between samples, read after each one; analog alone, a chunk.
            int count = digital ? 1 : Math.min(CHUNK, samples - done);
            for (LiveCircuitSolver.Sample sample : solver.advance(count)) {
                times[done] = sample.time();
                for (int node = 0; node < nodes.size(); node++) {
                    voltages.get(nodes.get(node))[done] = sample.voltages()[node];
                }
                if (sample.terminals() != null) {
                    int index = done;
                    sample.terminals().forEach((part, terminals) -> {
                        terminals.forEach((terminal, current) -> {
                            String column = CircuitComputations.partCurrentColumn(part, terminal);
                            if (column != null) {
                                currents.computeIfAbsent(column, ignored -> new double[samples])[index] = current;
                            }
                        });
                        // A two-terminal part reports under its own name too — the current into its first pin —, as a
                        // primitive does: a generator is `G1` as well as `G1.positive`, so `current G1` reads either.
                        if (terminals.size() == 2 && !terminals.containsKey("+")) {
                            currents.computeIfAbsent(part, ignored -> new double[samples])[index] = terminals.values().iterator().next();
                        }
                    });
                }
                levels.add(digital ? solver.levels() : Map.of());
                done++;
            }
            if (done % CHUNK == 0) {
                progress.report((double) done / samples, "Stepping the circuit");
            }
        }
        return new TransientRun(times, voltages, currents, levels);
    }

    /**
     * The parts a transient reports the current of without being asked for every part: whatever owns a branch of
     * its own — a source, a generator, an inductor, an ammeter —, not a branch inside a model, and every diode.
     */
    private static Set<String> partsWithCurrentsOfTheirOwn(Netlist netlist) {
        Set<String> parts = new LinkedHashSet<>();
        netlist.branches().stream().filter(branch -> !branch.contains(":")).forEach(parts::add);
        netlist.elements().stream().filter(element -> element.type() == CircuitElementType.DIODE)
                .forEach(element -> parts.add(element.id()));
        return parts;
    }

    /** One instant of a circuit, solved by other means — its operating point —, read the way a run is. */
    static TransientRun instant(Map<String, double[]> voltages, Map<String, double[]> currents) {
        return new TransientRun(new double[]{0}, voltages, currents, List.of(Map.of()));
    }

    public int samples() {
        return times.length;
    }

    public double[] times() {
        return times;
    }

    /** Every analog node's voltage, by node, in the engine's order. */
    public Map<String, double[]> voltages() {
        return voltages;
    }

    /** Every part's current, by its column. */
    public Map<String, double[]> currents() {
        return currents;
    }

    /** Every digital node's level at each sample; empty maps for a circuit with none. */
    public List<Map<String, Object>> levels() {
        return levels;
    }

    /** A node's voltage; node 0 is ground. */
    public double[] voltage(String node) {
        if (node.equals("0")) {
            return new double[times.length];
        }
        double[] values = voltages.get(node);
        if (values == null) {
            throw new IllegalArgumentException("'" + node + "' is not an analog node of the circuit");
        }
        return values;
    }

    /** A part's current — into its first terminal for a two-terminal part —, by the part's name. */
    public double[] current(String column) {
        double[] values = currents.get(column);
        if (values == null) {
            throw new IllegalArgumentException("'" + column + "' carries no current the circuit reports");
        }
        return values;
    }
}
