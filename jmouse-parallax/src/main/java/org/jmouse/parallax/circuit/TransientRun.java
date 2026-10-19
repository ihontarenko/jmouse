package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A circuit's transient worked out at once — what a computed graph, an object page and a datasheet check read —, by the
 * one engine the live bench runs ({@link LiveCircuitSolver}, keeping no part's energy): SPICE's step control, the
 * cut into pieces, models compiled to code. Samples are evenly spaced from 0 to the circuit's duration, both ends
 * included; each is read off the solution, never forcing a step to land on it. Each node's peaks over every sample's
 * interval are kept with it: what the steps between two samples went through, which no sample shows.
 *
 * <p>What it holds is named as the circuit names it: a node's voltage by the node, a part's current by
 * {@link ColumnNames#partCurrent} ({@code V1}, {@code D1}, {@code U1.output}), a digital node's level by
 * the node.</p>
 */
public final class TransientRun {

    /** Samples are asked of the engine this many at a time, as it allows. */
    private static final int CHUNK = 256;

    private final double[]                  times;
    private final Map<String, double[]>     voltages;
    /** Each node's lowest and highest voltage over each sample's interval, by node; empty for an instant. */
    private final Map<String, double[]>     lows;
    private final Map<String, double[]>     highs;
    private final Map<String, double[]>     currents;
    private final Map<String, double[]>     powers;
    private final List<Map<String, LogicLevel>> levels;

    private TransientRun(double[] times, Map<String, double[]> voltages, Map<String, double[]> lows,
                         Map<String, double[]> highs, Map<String, double[]> currents, Map<String, double[]> powers,
                         List<Map<String, LogicLevel>> levels) {
        this.times = times;
        this.voltages = voltages;
        this.lows = lows;
        this.highs = highs;
        this.currents = currents;
        this.powers = powers;
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
        return of(ElaboratedCircuit.of(object), samples, everyPart, null, progress);
    }

    /**
     * The same, of a circuit already elaborated, over the whole run or over a {@code window} of it — its first and last
     * instant, seconds; null for the whole. A window is reached by solving the run up to it, reading nothing, and is
     * sampled as finely as {@code samples} across it make: a switch's edge a few nanoseconds long, inside a run of
     * milliseconds, drawn as it happens and not as a line between two samples.
     */
    static TransientRun of(ElaboratedCircuit elaborated, int samples, boolean everyPart, double[] window,
                           ComputationProgress progress) {
        if (samples < 2) {
            throw new IllegalArgumentException("A transient has at least two samples.");
        }
        Netlist netlist = Netlist.of(elaborated);
        double duration = netlist.duration();
        double from = window == null ? 0 : window[0];
        double to = window == null ? duration : Math.min(window[1], duration);
        if (!(from >= 0) || !(to > from)) {
            throw new IllegalArgumentException("A window runs forward from 0 within the circuit's " + duration + " s.");
        }
        LiveCircuitSolver solver = new LiveCircuitSolver(elaborated, (to - from) / (samples - 1)).solutionWithPeaks();
        solver.trace(1);
        if (from > 0) {
            solver.skipTo(from);
        }
        solver.traceOnly(everyPart ? null : partsWithCurrentsOfTheirOwn(netlist));
        List<String> nodes = solver.nodes();
        double[] times = new double[samples];
        Map<String, double[]> voltages = new LinkedHashMap<>();
        Map<String, double[]> lows = new LinkedHashMap<>();
        Map<String, double[]> highs = new LinkedHashMap<>();
        // Each node's columns by the solver's node order: filled sample by sample, never looked up by name.
        double[][] nodeColumns = new double[nodes.size()][];
        double[][] lowColumns = new double[nodes.size()][];
        double[][] highColumns = new double[nodes.size()][];
        for (int node = 0; node < nodes.size(); node++) {
            nodeColumns[node] = new double[samples];
            lowColumns[node] = new double[samples];
            highColumns[node] = new double[samples];
            voltages.put(nodes.get(node), nodeColumns[node]);
            lows.put(nodes.get(node), lowColumns[node]);
            highs.put(nodes.get(node), highColumns[node]);
        }
        Map<String, double[]> currents = new LinkedHashMap<>();
        Map<String, double[]> powers = new LinkedHashMap<>();
        List<Map<String, LogicLevel>> levels = new ArrayList<>(samples);
        boolean digital = !solver.levels().isEmpty();
        int done = 0;
        while (done < samples) {
            // A digital node's level is the engine's state between samples, read after each one; analog alone, a chunk.
            int count = digital ? 1 : Math.min(CHUNK, samples - done);
            for (LiveSample sample : solver.advance(count)) {
                times[done] = sample.time();
                double[] sampled = sample.voltages();
                SampleEnvelope envelope = sample.envelope();
                for (int node = 0; node < nodeColumns.length; node++) {
                    nodeColumns[node][done] = sampled[node];
                    lowColumns[node][done] = envelope == null ? sampled[node] : envelope.lows()[node];
                    highColumns[node][done] = envelope == null ? sampled[node] : envelope.highs()[node];
                }
                if (sample.terminals() != null) {
                    int index = done;
                    sample.terminals().forEach((part, terminals) -> {
                        terminals.forEach((terminal, current) -> {
                            String column = ColumnNames.partCurrent(part, terminal);
                            if (column != null) {
                                currents.computeIfAbsent(column, ignored -> new double[samples])[index] = current;
                            }
                        });
                        // A two-terminal part reports under its own name too — the current into its first pin —, as a
                        // primitive does: a generator is `G1` as well as `G1.positive`, so `current G1` reads either.
                        if (terminals.size() == 2 && !terminals.containsKey(ObjectDefinition.PRIMITIVE_POSITIVE)) {
                            currents.computeIfAbsent(part, ignored -> new double[samples])[index] = terminals.values().iterator().next();
                        }
                    });
                }
                if (sample.powers() != null) {
                    int index = done;
                    sample.powers().forEach((part, power) ->
                            powers.computeIfAbsent(ColumnNames.partPower(part), ignored -> new double[samples])[index] = power);
                }
                levels.add(digital ? solver.levels() : Map.of());
                done++;
            }
            if (done % CHUNK == 0) {
                progress.report((double) done / samples, "Stepping the circuit");
            }
        }
        return new TransientRun(times, voltages, lows, highs, currents, powers, levels);
    }

    /**
     * The parts a transient reports the current of without being asked for every part: whatever owns a branch of
     * its own — a source, a generator —, not a branch inside a model.
     */
    private static Set<String> partsWithCurrentsOfTheirOwn(Netlist netlist) {
        Set<String> parts = new LinkedHashSet<>();
        netlist.branches().stream().filter(Netlist.Branch::partCurrent).map(Netlist.Branch::name).forEach(parts::add);
        return parts;
    }

    /** One instant of a circuit, solved by other means — its operating point —, read the way a run is. */
    static TransientRun instant(Map<String, double[]> voltages, Map<String, double[]> currents) {
        return new TransientRun(new double[]{0}, voltages, Map.of(), Map.of(), currents, Map.of(), List.of(Map.of()));
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

    /** Each node's lowest voltage over each sample's interval, by node; none for an instant. */
    public Map<String, double[]> lows() {
        return lows;
    }

    /** Each node's highest voltage over each sample's interval, by node; none for an instant. */
    public Map<String, double[]> highs() {
        return highs;
    }

    /** Every part's current, by its column. */
    public Map<String, double[]> currents() {
        return currents;
    }

    /** Each traced part's power, sample by sample, in watts, by {@link ColumnNames#partPower}. */
    public Map<String, double[]> powers() {
        return powers;
    }

    /** Every digital node's level at each sample; empty maps for a circuit with none. */
    public List<Map<String, LogicLevel>> levels() {
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
