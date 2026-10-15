package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentHierarchy;
import org.jmouse.parallax.object.ObjectDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * A transient that keeps running: each call advances the circuit sample by sample, for as long as someone watches.
 *
 * <p>The circuit is first cut into the parts that cannot affect one another ({@link CircuitPartitions}) and each part is
 * integrated on its own ({@link LiveIntegrator}), with its own step: an edge in one part does not shorten the steps
 * of another. Every part samples at the same instants, so a sample of the whole is the parts' samples together.</p>
 */
public final class LiveCircuitSolver {

    /**
     * A difference of two nodes a probe reads across a part: the positive node less the negative one, each an index into
     * the nodes it is numbered among, −1 for ground.
     */
    public record NodeDifference(int positive, int negative) {
    }

    /**
     * One sample: when, every node's voltage then, in the order of {@link #nodes()}, and — on a traced sample
     * ({@link #trace}) — the current into each part's terminals, by part and terminal; null on any other.
     */
    public record Sample(double time, double[] voltages, Map<String, Map<String, Double>> terminals,
                         Map<String, Metered> metered, Window window, Envelope envelope) {

        public Sample(double time, double[] voltages, Map<String, Map<String, Double>> terminals, Map<String, Metered> metered) {
            this(time, voltages, terminals, metered, null, null);
        }

        public Sample(double time, double[] voltages, Map<String, Map<String, Double>> terminals, Map<String, Metered> metered,
                      Window window) {
            this(time, voltages, terminals, metered, window, null);
        }
    }

    /**
     * The lowest and highest voltage of every node since the sample before, over every step the solver took between —
     * what a digital oscilloscope's peak detection keeps. A sample is one instant; a 20 kHz square read once every
     * 100 µs lands on either level at random and draws a saw that is not there. In the order of {@link #nodes()}.
     * {@code means} is each node's mean over the same interval — an averaging oscilloscope's sample: the same square
     * read once every 10 µs, five times a period, averaged 2 V for 1.5.
     */
    public record Envelope(double[] lows, double[] highs, double[] means, double[] differenceLows, double[] differenceHighs) {

        /**
         * {@code differenceLows} and {@code differenceHighs}: the peaks of each difference watched ({@link #difference}),
         * in the order given, NaN where none could be kept. A probe across a part reads the difference of two nodes, and
         * its peaks are not the difference of theirs: two nodes moving apart made the lowest of one less the highest of
         * the other a band twice as wide as the signal, thickest on its slopes.
         */
        public Envelope(double[] lows, double[] highs, double[] means) {
            this(lows, highs, means, new double[0], new double[0]);
        }
    }

    /**
     * What a traced sample stands for when the trace keeps one sample in many: the stretch since the last traced one —
     * every node's mean voltage and RMS over it, in the order of {@link #nodes()}, and every terminal's RMS current; the
     * sample's {@code terminals} are then the mean currents. Null on a sample that stands for its own instant.
     */
    public record Window(double[] means, double[] rms, Map<String, Map<String, Double>> terminalRms) {
    }

    /**
     * A metered part at a sample ({@link #meter}): the current into each of its terminals, the power it takes — negative
     * for a source delivering it —, and its states, by name.
     */
    public record Metered(Map<String, Double> terminals, double power, Map<String, Double> states) {

        public Metered {
            states = states == null ? Map.of() : states;
        }

        public Metered(Map<String, Double> terminals, double power) {
            this(terminals, power, Map.of());
        }

        /** Two pieces of one part, together. */
        Metered plus(Metered other) {
            Map<String, Double> sum = new LinkedHashMap<>(terminals);
            other.terminals.forEach((terminal, current) -> sum.merge(terminal, current, Double::sum));
            Map<String, Double> both = new LinkedHashMap<>(states);
            both.putAll(other.states);
            return new Metered(sum, power + other.power, both);
        }
    }

    private final List<LiveIntegrator> parts;
    /** Where each part's nodes stand among the whole circuit's. */
    private final int[][] placement;
    /** Per watched difference ({@link #difference}), in the order watched: the piece that keeps it; null for none. */
    private DifferenceOwner[] differenceOwners = new DifferenceOwner[0];

    /** Where a watched difference is kept: the piece both its nodes are in, and its place among that piece's differences. */
    private record DifferenceOwner(int part, int place) {
    }
    private final List<String> nodes;
    private final double interval;
    /** The bench temperature the parts warm up from, in °C. */
    private final double ambient;
    /** How each part warms and where it gives up, as its component writes it. */
    private final Map<String, PartThermal> thermals;
    /** The parts that have failed on a realistic bench, and how. */
    private final Map<String, String> failures;
    /** How warm each part is now, carried from one reading to the next — and to the solver that goes on from this one. */
    private final Map<String, Double> temperatures = new LinkedHashMap<>();
    /** The parts on the sheet simulated as the parts they are made of: what they report is theirs, added up. */
    private final List<ComponentHierarchy.Assembly> assemblies;
    private double warmedAt;

    /**
     * @param sampleInterval the time between two samples, in seconds
     */
    public LiveCircuitSolver(CompiledObject object, double sampleInterval) {
        this(CircuitPartitions.of(object).stream().map(part -> new LiveIntegrator(part, sampleInterval, LiveIntegrator.benchOf(object))).toList(),
                sampleInterval, object);
    }

    private LiveCircuitSolver(List<LiveIntegrator> parts, double interval, CompiledObject object) {
        this.parts = List.copyOf(parts);
        this.interval = interval;
        this.ambient = ambientOf(object);
        this.thermals = PartThermal.of(object);
        this.assemblies = ComponentHierarchy.assemblies(object);
        this.failures = object.definition() instanceof ObjectDefinition.Circuit circuit && circuit.conditions() != null
                ? circuit.conditions().failures() : Map.of();
        Set<String> all = new LinkedHashSet<>();
        parts.forEach(part -> all.addAll(part.nodes()));
        this.nodes = List.copyOf(all);
        this.placement = new int[this.parts.size()][];
        for (int part = 0; part < this.parts.size(); part++) {
            List<String> own = this.parts.get(part).nodes();
            placement[part] = own.stream().mapToInt(nodes::indexOf).toArray();
        }
    }

    /**
     * The circuit with new values, going on from where {@code previous} is — every part from its own state.
     *
     * @throws IllegalArgumentException when it cannot go on: its parts are not the ones it had
     */
    /**
     * Keeps no readings — no part's energy, no peaks or means —: for a caller that reads the solution and not a screen,
     * a computed transient or a benchmark of the engine. The bench keeps them, and pays for them.
     */
    /** The level every digital node stands at now, over all the pieces. */
    public synchronized Map<String, Object> levels() {
        Map<String, Object> levels = new LinkedHashMap<>();
        parts.forEach(part -> levels.putAll(part.levels()));
        return levels;
    }

    public LiveCircuitSolver solutionOnly() {
        parts.forEach(LiveIntegrator::solutionOnly);
        return this;
    }

    public static LiveCircuitSolver continuing(CompiledObject object, LiveCircuitSolver previous) {
        return continuing(object, previous, previous.interval);
    }

    /** The same circuit going on from where {@code previous} is, sampled every {@code sampleInterval} from now on. */
    public static LiveCircuitSolver continuing(CompiledObject object, LiveCircuitSolver previous, double sampleInterval) {
        List<CompiledObject> split = CircuitPartitions.of(object);
        if (split.size() != previous.parts.size()) {
            throw new IllegalArgumentException("The circuit changed its parts; it starts again.");
        }
        List<LiveIntegrator> continued = new ArrayList<>();
        for (int index = 0; index < split.size(); index++) {
            continued.add(LiveIntegrator.continuing(split.get(index), previous.parts.get(index), sampleInterval,
                    LiveIntegrator.benchOf(object)));
        }
        LiveCircuitSolver solver = new LiveCircuitSolver(continued, sampleInterval, object);
        solver.temperatures.putAll(previous.temperatures);
        solver.warmedAt = previous.warmedAt;
        return solver;
    }

    private static double ambientOf(CompiledObject object) {
        return object.definition() instanceof ObjectDefinition.Circuit circuit && circuit.conditions() != null
                ? circuit.conditions().temperature() : ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE;
    }

    /**
     * How hard every part worked since this was last asked — its average power, its current now, the temperature it
     * would settle at —, by part. A part the cut split between two pieces (a supply both use) adds up.
     */
    public synchronized Map<String, PartLoad> loads() {
        Map<String, double[]> merged = new LinkedHashMap<>();
        Map<String, Map<String, Double>> terminals = new LinkedHashMap<>();
        for (LiveIntegrator part : parts) {
            for (LiveIntegrator.PartReading reading : part.readParts()) {
                double[] sum = merged.computeIfAbsent(reading.part(), ignored -> new double[] {0, 0, Double.NaN});
                sum[0] += reading.power();
                sum[1] = Math.max(sum[1], reading.thermalResistance());
                sum[2] = hotter(sum[2], reading.heating());
                Map<String, Double> into = terminals.computeIfAbsent(reading.part(), ignored -> new LinkedHashMap<>());
                reading.terminals().forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
            }
        }
        // A part made of other parts works as hard as they do together, and its pins carry what crosses its boundary.
        for (ComponentHierarchy.Assembly assembly : assemblies) {
            double[] sum = merged.computeIfAbsent(assembly.part(), ignored -> new double[] {0, 0, Double.NaN});
            assembly.members().keySet().forEach(member -> {
                double[] own = merged.get(member);
                if (own != null) {
                    sum[0] += own[0];
                    sum[1] = Math.max(sum[1], own[1]);
                    sum[2] = hotter(sum[2], own[2]);
                }
            });
            terminals.put(assembly.part(), assembly.pinCurrents(terminals));
        }
        double now = time();
        double seconds = now - warmedAt;
        warmedAt = now;
        Map<String, PartLoad> loads = new LinkedHashMap<>();
        merged.forEach((name, sum) -> {
            // What a failed part became is not a part of the circuit; the part itself is reported below.
            if (name.contains("_failed_")) {
                return;
            }
            PartThermal thermal = thermals.containsKey(name) ? thermals.get(name)
                    : sum[1] > 0 ? new PartThermal(sum[1], PartThermal.DEFAULT_TIME_CONSTANT, Double.NaN, Double.NaN, "open", 0) : null;
            double temperature = Double.NaN;
            if (!Double.isNaN(sum[2])) {
                // A model that keeps its own heat is as hot as its equations say: no estimate beside them.
                temperature = ambient + sum[2];
                temperatures.put(name, temperature);
            } else if (thermal != null && thermal.resistance() > 0) {
                temperature = thermal.warmed(temperatures.getOrDefault(name, ambient), sum[0], ambient, seconds);
                temperatures.put(name, temperature);
            }
            loads.put(name, new PartLoad(sum[0], terminals.get(name), temperature,
                    thermal == null ? Double.NaN : thermal.maximum(), thermal == null ? Double.NaN : thermal.rating(), null));
        });
        // A failed part takes nothing and cools; it is still there, burnt.
        failures.forEach((name, failure) -> {
            PartThermal thermal = thermals.get(name);
            double temperature = Double.NaN;
            if (thermal != null && thermal.resistance() > 0) {
                temperature = thermal.warmed(temperatures.getOrDefault(name, ambient), 0, ambient, seconds);
                temperatures.put(name, temperature);
            }
            loads.put(name, new PartLoad(0, Map.of(), temperature, thermal == null ? Double.NaN : thermal.maximum(),
                    thermal == null ? Double.NaN : thermal.rating(), failure));
        });
        return loads;
    }

    /** The larger of two heatings, NaN only where both are. */
    private static double hotter(double one, double other) {
        return Double.isNaN(one) ? other : Double.isNaN(other) ? one : Math.max(one, other);
    }

    /** How each part fails when it overheats, as its component writes it; {@code open} for a part that does not say. */
    public String failureOf(String part) {
        PartThermal thermal = thermals.get(part);
        return thermal == null ? "open" : thermal.failure();
    }

    /** The node names, in the order a sample reports them. */
    public List<String> nodes() {
        return nodes;
    }

    public double interval() {
        return interval;
    }

    public synchronized double time() {
        return parts.getFirst().time();
    }

    /** The next {@code count} samples, the first at the time the last call ended. */
    public synchronized List<Sample> advance(int count) {
        List<List<Sample>> byPart = new ArrayList<>();
        for (LiveIntegrator part : parts) {
            byPart.add(part.advance(count));
        }
        return merged(byPart, count);
    }

    /**
     * Goes through the next {@code count} samples and returns only the traced ones: the circuit, its energy and its
     * trace go on, the samples no screen keeps are never read. Every piece traces the same samples, counted from the
     * start, so they line up.
     */
    public synchronized List<Sample> pass(long count) {
        List<List<Sample>> byPart = new ArrayList<>();
        for (LiveIntegrator part : parts) {
            byPart.add(part.pass(count));
        }
        return merged(byPart, byPart.getFirst().size());
    }

    /** The pieces' samples, index by index, as the whole circuit's. */
    private List<Sample> merged(List<List<Sample>> byPart, int count) {
        if (byPart.size() == 1) {
            if (assemblies.isEmpty() && differenceOwners.length == 0) {
                return byPart.getFirst();
            }
            return byPart.getFirst().stream()
                    .map(sample -> new Sample(sample.time(), sample.voltages(), assembled(sample.terminals()), sample.metered(),
                            assembled(sample.window()), withDifferences(sample.envelope()))).toList();
        }
        List<Sample> merged = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double[] voltages = new double[nodes.size()];
            Map<String, Map<String, Double>> terminals = null;
            Map<String, Metered> metered = null;
            for (int part = 0; part < byPart.size(); part++) {
                Sample own = byPart.get(part).get(index);
                for (int node = 0; node < own.voltages().length; node++) {
                    voltages[placement[part][node]] = own.voltages()[node];
                }
                // Every piece traces the same samples; a part the cut split between two pieces adds up, as in loads().
                if (own.terminals() != null) {
                    terminals = terminals == null ? new LinkedHashMap<>() : terminals;
                    for (Map.Entry<String, Map<String, Double>> reading : own.terminals().entrySet()) {
                        Map<String, Double> into = terminals.computeIfAbsent(reading.getKey(), ignored -> new LinkedHashMap<>());
                        reading.getValue().forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
                    }
                }
                if (own.metered() != null) {
                    metered = metered == null ? new LinkedHashMap<>() : metered;
                    // A part the cut split between two pieces adds up, as its terminals and its power do in loads().
                    for (Map.Entry<String, Metered> reading : own.metered().entrySet()) {
                        metered.merge(reading.getKey(), reading.getValue(), Metered::plus);
                    }
                }
            }
            merged.add(new Sample(byPart.getFirst().get(index).time(), voltages, assembled(terminals), metered, assembled(mergedWindow(byPart, index)),
                    mergedEnvelope(byPart, index)));
        }
        return merged;
    }

    /** The pieces' peaks at one sample as the whole circuit's, each node where its piece has it; none unless every piece read them. */
    private Envelope mergedEnvelope(List<List<Sample>> byPart, int index) {
        double[] lows = new double[nodes.size()];
        double[] highs = new double[nodes.size()];
        double[] means = new double[nodes.size()];
        List<Envelope> pieces = new ArrayList<>();
        for (int part = 0; part < byPart.size(); part++) {
            Envelope own = byPart.get(part).get(index).envelope();
            if (own == null) {
                return null;
            }
            pieces.add(own);
            for (int node = 0; node < own.lows().length; node++) {
                lows[placement[part][node]] = own.lows()[node];
                highs[placement[part][node]] = own.highs()[node];
                means[placement[part][node]] = own.means()[node];
            }
        }
        double[][] differences = mergedDifferences(pieces);
        return new Envelope(lows, highs, means, differences[0], differences[1]);
    }

    /**
     * The pieces' windows at one sample as the whole circuit's: each node's mean and RMS where the piece has it, a part
     * the cut split adding up — its RMS only roughly, as two currents' RMS do not add. None unless every piece has one.
     */
    private Window mergedWindow(List<List<Sample>> byPart, int index) {
        double[] means = new double[nodes.size()];
        double[] rms = new double[nodes.size()];
        Map<String, Map<String, Double>> terminalRms = new LinkedHashMap<>();
        for (int part = 0; part < byPart.size(); part++) {
            Window own = byPart.get(part).get(index).window();
            if (own == null) {
                return null;
            }
            for (int node = 0; node < own.means().length; node++) {
                means[placement[part][node]] = own.means()[node];
                rms[placement[part][node]] = own.rms()[node];
            }
            own.terminalRms().forEach((name, currents) -> {
                Map<String, Double> into = terminalRms.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
                currents.forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
            });
        }
        return new Window(means, rms, terminalRms);
    }

    /** A window with every part made of other parts reporting its pins' RMS as well — roughly, as {@link #mergedWindow} adds. */
    private Window assembled(Window window) {
        return window == null ? null : new Window(window.means(), window.rms(), assembled(window.terminalRms()));
    }

    /** A sample's terminal currents with every part made of other parts reporting into its own pins as well. */
    private Map<String, Map<String, Double>> assembled(Map<String, Map<String, Double>> terminals) {
        if (terminals == null || assemblies.isEmpty()) {
            return terminals;
        }
        Map<String, Map<String, Double>> assembled = new LinkedHashMap<>(terminals);
        assemblies.forEach(assembly -> assembled.put(assembly.part(), assembly.pinCurrents(terminals)));
        return assembled;
    }

    /** Reads these parts on every sample — what a meter on them shows —; an empty set reads none. */
    public synchronized void meter(Set<String> metered) {
        parts.forEach(part -> part.meter(metered));
    }

    /**
     * Keeps the peaks of these differences of two nodes — positive and negative, as indices into {@link #nodes()}, −1 for
     * ground — over every step, as each node's own are kept. A difference is kept by the piece both its nodes are in; one
     * whose nodes lie in two pieces, or a node the circuit lacks, has none (NaN in {@link Envelope#differenceLows}).
     */
    public synchronized void difference(List<NodeDifference> pairs) {
        differenceOwners = new DifferenceOwner[pairs.size()];
        List<List<NodeDifference>> byPart = new ArrayList<>();
        parts.forEach(part -> byPart.add(new ArrayList<>()));
        for (int pair = 0; pair < pairs.size(); pair++) {
            for (int part = 0; part < parts.size(); part++) {
                int positive = localIndex(part, pairs.get(pair).positive());
                int negative = localIndex(part, pairs.get(pair).negative());
                if (positive != -2 && negative != -2 && !(positive == -1 && negative == -1)) {
                    differenceOwners[pair] = new DifferenceOwner(part, byPart.get(part).size());
                    byPart.get(part).add(new NodeDifference(positive, negative));
                    break;
                }
            }
        }
        for (int part = 0; part < parts.size(); part++) {
            parts.get(part).difference(byPart.get(part));
        }
    }

    /** A node of the whole circuit as a piece numbers it: −1 for ground, −2 when the piece does not have it. */
    private int localIndex(int part, int node) {
        if (node == -1) {
            return -1;
        }
        for (int local = 0; local < placement[part].length; local++) {
            if (placement[part][local] == node) {
                return local;
            }
        }
        return -2;
    }

    /** A single piece's envelope with its differences in the order watched — a pair it could not keep is missing from its own. */
    private Envelope withDifferences(Envelope own) {
        if (own == null || differenceOwners.length == 0) {
            return own;
        }
        double[][] differences = mergedDifferences(List.of(own));
        return new Envelope(own.lows(), own.highs(), own.means(), differences[0], differences[1]);
    }

    /** The watched differences' peaks at one sample, in the order watched, from the pieces that keep them. */
    private double[][] mergedDifferences(List<Envelope> pieces) {
        double[] lows = new double[differenceOwners.length];
        double[] highs = new double[differenceOwners.length];
        for (int pair = 0; pair < differenceOwners.length; pair++) {
            DifferenceOwner owner = differenceOwners[pair];
            Envelope own = owner == null ? null : pieces.get(owner.part());
            boolean kept = own != null && owner.place() < own.differenceLows().length;
            lows[pair] = kept ? own.differenceLows()[owner.place()] : Double.NaN;
            highs[pair] = kept ? own.differenceHighs()[owner.place()] : Double.NaN;
        }
        return new double[][]{lows, highs};
    }

    /** Reads every part's terminal currents on every {@code every}-th sample as well; 0 reads none. */
    public synchronized void trace(int every) {
        parts.forEach(part -> part.trace(every));
    }

    /** Traces only these parts' terminal currents; null traces every part — what a sheet that draws them all needs. */
    public synchronized void traceOnly(java.util.Set<String> only) {
        parts.forEach(part -> part.traceOnly(only));
    }
}
