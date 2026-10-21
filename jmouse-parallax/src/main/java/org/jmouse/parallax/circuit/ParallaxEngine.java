package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;
import org.jmouse.parallax.object.CompiledObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

/**
 * This library's own engine for the batch analyses: the transient by the live engine ({@link TransientRun}) — one
 * engine for the bench, the computed graphs and the datasheet checks —, the operating point, the DC sweep and the
 * small-signal analyses by {@link CircuitSolver}. It is measured against ngspice, never assumed to agree with it.
 */
public final class ParallaxEngine implements CircuitEngine {

    /** How much higher (or lower) than both neighbours a grid point has to be to be refined as a peak (or a notch), dB. */
    private static final double EXTREME_MARGIN = 1e-4;
    private static final int MAXIMUM_REFINED_EXTREMES = 64;
    private static final double GOLDEN = (Math.sqrt(5) - 1) / 2;
    /** Each narrows the bracket by 0.618: twenty from a grid step to a 10⁻⁴ share of it. */
    private static final int GOLDEN_ITERATIONS = 20;

    @Override
    public CircuitEngineKind kind() {
        return CircuitEngineKind.PARALLAX;
    }

    @Override
    public Dataset operatingPoint(CompiledObject circuit) {
        Netlist netlist = Netlist.of(circuit);
        CircuitSolver solver = new CircuitSolver(netlist);
        double[] state = solver.operatingPoint();
        // Named as the circuit names them; the unit says what each is: a node's volts, a part's amperes, a resistor's watts.
        Map<String, Double> values = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (int node = 0; node < netlist.nodes().size(); node++) {
            values.put(netlist.nodes().get(node), state[node]);
            units.put(netlist.nodes().get(node), ColumnNames.VOLTS);
        }
        for (int branch = 0; branch < netlist.branches().size(); branch++) {
            Netlist.Branch own = netlist.branches().get(branch);
            if (own.partCurrent()) {
                values.put(own.name(), state[netlist.nodes().size() + branch]);
                units.put(own.name(), ColumnNames.AMPERES);
            }
        }
        PartReadings.currents(solver.devices(), state).forEach((column, current) -> {
            if (values.putIfAbsent(column, current) == null) {
                units.put(column, ColumnNames.AMPERES);
            }
        });
        PartReadings.powers(solver.devices(), state).forEach((column, power) -> {
            values.put(column, power);
            units.put(column, ColumnNames.WATTS);
        });
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("units", "a node in volts; a part's current in amperes (R1, U1.pin), its power in watts (R1.power)");
        return new Dataset.Measurements(values, notes, units);
    }

    /**
     * Worked out by the live engine, keeping only each node's peaks ({@link TransientRun}). Columns: every node's
     * voltage, with its peaks between the samples, then the current of every part that carries one of its own (a source,
     * a generator, an inductor, a diode) — or, with {@code everyPart}, of every part and every model pin, for a sheet
     * that draws them all —, and the parts' powers. With {@code levels}, the digital nodes' logic levels instead.
     */
    @Override
    public Dataset transientResponse(CompiledObject circuit, TransientRequest request, ComputationProgress progress) {
        ElaboratedCircuit elaborated = ElaboratedCircuit.of(circuit);
        Netlist netlist = Netlist.of(elaborated);
        if (request.levels() && netlist.digitalNodes().isEmpty()) {
            throw new IllegalArgumentException("The circuit has no digital nodes to show the logic levels of.");
        }
        boolean everyPart = request.everyPart() && !request.levels();
        TransientRun run = TransientRun.of(elaborated, request.samples(), everyPart, request.window(), progress);
        float[] times = floats(run.times());
        if (request.levels()) {
            return logicLevels(times, netlist.digitalNodes(), run.levels());
        }
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        Map<String, Dataset.TimeSeries.Band> peaks = new LinkedHashMap<>();
        run.voltages().forEach((node, voltages) -> {
            columns.add(node);
            values.add(floats(voltages));
            units.add(ColumnNames.VOLTS);
            peaks.put(node, new Dataset.TimeSeries.Band(floats(run.lows().get(node)), floats(run.highs().get(node))));
        });
        run.currents().forEach((column, currents) -> {
            // Asked for no more than the parts with a current of their own: those, by the part's name alone.
            if (everyPart || !column.contains(".")) {
                columns.add(column);
                values.add(floats(currents));
                units.add(ColumnNames.AMPERES);
            }
        });
        run.powers().forEach((column, powers) -> {
            columns.add(column);
            values.add(floats(powers));
            units.add(ColumnNames.WATTS);
        });
        return new Dataset.TimeSeries("t", times, columns, values, units, Dataset.TimeSeries.AxisScale.LINEAR, peaks);
    }

    @Override
    public Dataset frequencyResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress) {
        Netlist netlist = Netlist.of(circuit);
        CircuitSolver solver = new CircuitSolver(netlist);
        double[] operatingPoint = smallSignalBias(solver, netlist);
        int nodes = netlist.nodes().size();
        TreeMap<Double, Complex[]> responses = new TreeMap<>();
        for (int point = 0; point < range.points(); point++) {
            double frequency = range.frequency(point);
            responses.put(frequency, solver.phasors(frequency, operatingPoint));
        }
        refineExtremes(responses, nodes, frequency -> solver.phasors(frequency, operatingPoint));
        int count = responses.size();
        float[] frequencies = new float[count];
        float[][] gains = new float[nodes][count];
        float[][] phases = new float[nodes][count];
        int point = 0;
        for (Map.Entry<Double, Complex[]> entry : responses.entrySet()) {
            frequencies[point] = entry.getKey().floatValue();
            for (int node = 0; node < nodes; node++) {
                gains[node][point] = (float) gainOf(entry.getValue()[node]);
                phases[node][point] = (float) Math.toDegrees(entry.getValue()[node].argument());
            }
            point++;
        }
        // Each node's gain and phase, named for the node; the unit says which is which.
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        for (int node = 0; node < nodes; node++) {
            columns.add(netlist.nodes().get(node) + ColumnNames.GAIN_SUFFIX);
            values.add(gains[node]);
            units.add(ColumnNames.DECIBELS);
            columns.add(netlist.nodes().get(node) + ColumnNames.PHASE_SUFFIX);
            values.add(phases[node]);
            units.add(ColumnNames.DEGREES);
        }
        return new Dataset.TimeSeries("f", frequencies, columns, values, units, Dataset.TimeSeries.AxisScale.LOGARITHMIC);
    }

    /**
     * The noise every node carries, V/√Hz over frequency: each device's noise currents at the operating point — a
     * resistor's 4kT/R, every {@code noise(density)} a model's equations add — driven through the small-signal circuit
     * one at a time, their powers added. SPICE's {@code .noise}, for every node at once.
     */
    @Override
    public Dataset noiseResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress) {
        Netlist netlist = Netlist.of(circuit);
        CircuitSolver solver = new CircuitSolver(netlist);
        double[] operatingPoint = smallSignalBias(solver, netlist);
        List<NoiseCurrent> sources = solver.noiseSources(operatingPoint);
        int points = range.points();
        int nodes = netlist.nodes().size();
        float[] frequencies = new float[points];
        float[][] densities = new float[nodes][points];
        for (int point = 0; point < points; point++) {
            double frequency = range.frequency(point);
            double[] powers = solver.noiseDensities(frequency, operatingPoint, sources);
            frequencies[point] = (float) frequency;
            for (int node = 0; node < nodes; node++) {
                densities[node][point] = (float) Math.sqrt(powers[node]);
            }
        }
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        for (int node = 0; node < nodes; node++) {
            columns.add(netlist.nodes().get(node) + ColumnNames.NOISE_SUFFIX);
            values.add(densities[node]);
        }
        return new Dataset.TimeSeries("f", frequencies, columns, values,
                Collections.nCopies(columns.size(), ColumnNames.NOISE_DENSITY), Dataset.TimeSeries.AxisScale.LOGARITHMIC);
    }

    @Override
    public Dataset directCurrentSweep(CompiledObject circuit, SweepRequest request, ComputationProgress progress) {
        if (request.sweepsParameter()) {
            return sweepParameter(circuit, request, progress);
        }
        Netlist netlist = Netlist.of(circuit);
        Netlist.Element source = sweptSource(netlist, request.source());
        int points = request.points();
        float[] axis = new float[points];
        double[][] states = new double[points][];
        List<Map<String, Double>> currents = new ArrayList<>(points);
        // One solver for the whole sweep — its system's ordering, its compiled models —, the swept source alone held at
        // each value in turn.
        CircuitSolver solver = new CircuitSolver(netlist);
        for (int point = 0; point < points; point++) {
            double value = request.value(point);
            axis[point] = (float) value;
            solver = solver.withSourceValue(source.id(), value);
            states[point] = point == 0 ? solver.operatingPoint() : solver.operatingPoint(states[point - 1]);
            // Read while the point's own linearization stands: a junction's current read off the last point's line was
            // that point's current, at every point.
            currents.add(PartReadings.currents(solver.devices(), states[point]));
            if (point % 64 == 0) {
                progress.report((double) point / points, "Sweeping " + source.id());
            }
        }
        return series(source.id(), axis, netlist, states, currents);
    }

    /**
     * The operating point at every value of one of the circuit's parameters: what turning a knob does to the circuit at
     * rest — a wiper from end to end opening a transistor. The circuit is built again for each value, as a slider moved
     * there would build it, and each point starts from the one before, so a switch-like curve is followed, not jumped.
     */
    private static Dataset sweepParameter(CompiledObject object, SweepRequest request, ComputationProgress progress) {
        String parameter = request.parameter();
        if (object.parameters().stream().noneMatch(definition -> definition.name().equals(parameter))) {
            throw new IllegalArgumentException("The circuit has no parameter '" + parameter + "' to sweep.");
        }
        int points = request.points();
        float[] axis = new float[points];
        double[][] states = new double[points][];
        List<Map<String, Double>> currents = new ArrayList<>(points);
        Netlist netlist = null;
        int shape = -1;
        for (int point = 0; point < points; point++) {
            double value = request.value(point);
            axis[point] = (float) value;
            netlist = Netlist.of(object.withParameters(Map.of(parameter, value)));
            // Every point is read through one netlist's node and branch layout (see series); a parameter that adds or
            // removes parts mid-sweep would make the columns incomparable, so refuse it rather than misalign.
            if (shape < 0) {
                shape = netlist.size();
            } else if (netlist.size() != shape) {
                throw new IllegalArgumentException("Sweeping '" + parameter
                        + "' changes the circuit's shape between values; sweep a parameter that does not add or remove parts.");
            }
            CircuitSolver solver = new CircuitSolver(netlist);
            boolean continued = point > 0 && states[point - 1].length == netlist.size();
            states[point] = continued ? solver.operatingPoint(states[point - 1]) : solver.operatingPoint();
            currents.add(PartReadings.currents(solver.devices(), states[point]));
            if (point % 16 == 0) {
                progress.report((double) point / points, "Sweeping " + parameter);
            }
        }
        return series(parameter, axis, netlist, states, currents);
    }

    private static Netlist.Element sweptSource(Netlist netlist, String id) {
        return netlist.elements().stream()
                .filter(element -> element.kind().isSource())
                .filter(element -> id.isEmpty() || element.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(id.isEmpty()
                        ? "The circuit has no source to sweep; sweep one of its parameters instead."
                        : "'" + id + "' is not a source of this circuit."));
    }

    /** One column per digital node, named as the node and in the unit `level`: what the event solver held it at, sample by sample. */
    private static Dataset.TimeSeries logicLevels(float[] times, List<String> digitalNodes, List<Map<String, LogicLevel>> held) {
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        for (String node : digitalNodes) {
            columns.add(node);
            float[] levels = new float[times.length];
            for (int sample = 0; sample < times.length; sample++) {
                levels[sample] = held.get(sample).getOrDefault(node, LogicLevel.RELEASED).datasetValue();
            }
            values.add(levels);
        }
        return new Dataset.TimeSeries("t", times, columns, values,
                Collections.nCopies(columns.size(), CircuitComputations.LEVEL_UNIT));
    }

    /** Node voltages, own-current branches and diode currents along an axis — time or a swept value. */
    private static Dataset series(String axisName, float[] axis, Netlist netlist, double[][] states,
                                  List<Map<String, Double>> currents) {
        List<String> columns = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        for (int node = 0; node < netlist.nodes().size(); node++) {
            columns.add(netlist.nodes().get(node));
            values.add(column(states, node));
            units.add(ColumnNames.VOLTS);
        }
        for (int branch = 0; branch < netlist.branches().size(); branch++) {
            Netlist.Branch own = netlist.branches().get(branch);
            if (own.partCurrent()) {
                columns.add(own.name());
                values.add(column(states, netlist.nodes().size() + branch));
                units.add(ColumnNames.AMPERES);
            }
        }
        for (String column : currents.isEmpty() ? List.<String>of() : List.copyOf(currents.getFirst().keySet())) {
            if (columns.contains(column)) {
                continue;
            }
            float[] series = new float[states.length];
            for (int sample = 0; sample < states.length; sample++) {
                series[sample] = (float) currents.get(sample).getOrDefault(column, Double.NaN).doubleValue();
            }
            columns.add(column);
            values.add(series);
            units.add(ColumnNames.AMPERES);
        }
        return new Dataset.TimeSeries(axisName, axis, columns, values, units);
    }

    /** A phasor's gain in decibels, a vanishing one held at −240 dB rather than −∞. */
    static double gainOf(Complex response) {
        return 20 * Math.log10(Math.max(response.modulus(), 1e-12));
    }

    /** Where a small-signal analysis linearizes: a linear circuit's model does not depend on its bias, so rest will do. */
    static double[] smallSignalBias(CircuitSolver solver, Netlist netlist) {
        return solver.nonlinear() ? solver.operatingPoint() : new double[netlist.size()];
    }

    /**
     * Finds every peak and notch the grid only brushed past and adds it: a resonance narrower than the grid's step falls
     * between two frequencies, and the Bode plot showed a Q of 10 at 19.07 dB instead of 20.01. Around each interior
     * extreme of a node's gain the frequency of the true extreme is found by golden-section search on the logarithm of
     * the frequency, and every frequency evaluated on the way joins the response — so the curve is drawn through the
     * summit, and the points around it are dense where it bends most.
     */
    private static void refineExtremes(TreeMap<Double, Complex[]> responses, int nodes, DoubleFunction<Complex[]> respond) {
        List<Double> grid = new ArrayList<>(responses.keySet());
        int refined = 0;
        for (int node = 0; node < nodes && refined < MAXIMUM_REFINED_EXTREMES; node++) {
            for (int index = 1; index < grid.size() - 1 && refined < MAXIMUM_REFINED_EXTREMES; index++) {
                double before = gainOf(responses.get(grid.get(index - 1))[node]);
                double here = gainOf(responses.get(grid.get(index))[node]);
                double after = gainOf(responses.get(grid.get(index + 1))[node]);
                boolean peak = here > before + EXTREME_MARGIN && here > after + EXTREME_MARGIN;
                boolean notch = here < before - EXTREME_MARGIN && here < after - EXTREME_MARGIN;
                if (!peak && !notch) {
                    continue;
                }
                int sign = peak ? 1 : -1;
                int column = node;
                double low = Math.log(grid.get(index - 1));
                double high = Math.log(grid.get(index + 1));
                DoubleUnaryOperator height = logarithm -> {
                    double frequency = Math.exp(logarithm);
                    Complex[] response = responses.computeIfAbsent(frequency, respond::apply);
                    return sign * gainOf(response[column]);
                };
                double first = high - GOLDEN * (high - low);
                double second = low + GOLDEN * (high - low);
                double firstHeight = height.applyAsDouble(first);
                double secondHeight = height.applyAsDouble(second);
                for (int iteration = 0; iteration < GOLDEN_ITERATIONS; iteration++) {
                    if (firstHeight > secondHeight) {
                        high = second;
                        second = first;
                        secondHeight = firstHeight;
                        first = high - GOLDEN * (high - low);
                        firstHeight = height.applyAsDouble(first);
                    } else {
                        low = first;
                        first = second;
                        firstHeight = secondHeight;
                        second = low + GOLDEN * (high - low);
                        secondHeight = height.applyAsDouble(second);
                    }
                }
                refined++;
            }
        }
    }

    private static float[] floats(double[] values) {
        float[] narrowed = new float[values.length];
        for (int index = 0; index < values.length; index++) {
            narrowed[index] = (float) values[index];
        }
        return narrowed;
    }

    private static float[] column(double[][] states, int index) {
        float[] column = new float[states.length];
        for (int sample = 0; sample < states.length; sample++) {
            column[sample] = (float) states[sample][index];
        }
        return column;
    }
}
