package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.elaboration.CircuitPartitions;
import org.jmouse.parallax.circuit.elaboration.ComponentHierarchy;
import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;
import org.jmouse.parallax.component.PhysicalConstants;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition.BenchConditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A transient that keeps running: each call advances the circuit sample by sample, for as long as someone watches.
 *
 * <p>The circuit is first cut into the pieces that cannot affect one another ({@link CircuitPartitions}) and each piece
 * is integrated on its own ({@link LiveIntegrator}), with its own step: an edge in one piece does not shorten the steps
 * of another. Every piece samples at the same instants, so a sample of the whole is the pieces' samples together.</p>
 */
public final class LiveCircuitSolver {

    /** A node a piece does not have: a difference over it is kept by another piece, or by none. */
    private static final int ABSENT = -2;

    private final List<LiveIntegrator> pieces;
    /** The circuit as it is simulated, built once: what a part's instruments and displays are read over. */
    private final ElaboratedCircuit elaborated;
    /** Where each piece's nodes stand among the whole circuit's. */
    private final int[][] placement;
    /** Per watched difference ({@link #difference}), in the order watched: the piece that keeps it; null for none. */
    private DifferenceOwner[] differenceOwners = new DifferenceOwner[0];
    private final List<String> nodes;
    private final double interval;
    /** Where each part gives up, as its component writes it. */
    private final Map<String, PartLimits> limits = new LinkedHashMap<>();
    /** The parts that have failed on a realistic bench, and how. */
    private final Map<String, BenchConditions.Failure> failures;
    /**
     * The heat of each failed part, cooling by its own law: what it became has no heat of its own, yet the part is still
     * there, burnt, and goes on cooling as its component says it does.
     */
    private final Map<String, PartHeat> cooling = new LinkedHashMap<>();
    /** Each part's states at the last {@link #loads()}: what the figures its component displays read. */
    private Map<String, Map<String, Double>> states = Map.of();
    /** The parts on the sheet simulated as the parts they are made of: what they report is theirs, added up. */
    private final List<ComponentHierarchy.Assembly> assemblies;
    private double warmedAt;

    /** Where a watched difference is kept: the piece both its nodes are in, and its place among that piece's differences. */
    private record DifferenceOwner(int piece, int place) {
    }

    /**
     * @param sampleInterval the time between two samples, in seconds
     */
    public LiveCircuitSolver(CompiledObject object, double sampleInterval) {
        this(ElaboratedCircuit.of(object), sampleInterval);
    }

    LiveCircuitSolver(ElaboratedCircuit elaborated, double sampleInterval) {
        this(CircuitPartitions.of(elaborated).stream().map(piece -> new LiveIntegrator(piece, sampleInterval)).toList(),
                sampleInterval, elaborated);
    }

    private LiveCircuitSolver(List<LiveIntegrator> pieces, double interval, ElaboratedCircuit elaborated) {
        this.pieces = List.copyOf(pieces);
        this.interval = interval;
        this.elaborated = elaborated;
        this.limits.putAll(PartLimits.of(elaborated));
        this.assemblies = elaborated.assemblies();
        this.failures = elaborated.bench() != null ? elaborated.bench().failures() : Map.of();
        Set<String> all = new LinkedHashSet<>();
        this.pieces.forEach(piece -> all.addAll(piece.nodes()));
        this.nodes = List.copyOf(all);
        this.placement = new int[this.pieces.size()][];
        for (int piece = 0; piece < this.pieces.size(); piece++) {
            placement[piece] = this.pieces.get(piece).nodes().stream().mapToInt(nodes::indexOf).toArray();
        }
    }

    /** The level every digital node stands at now, over all the pieces. */
    public synchronized Map<String, LogicLevel> levels() {
        Map<String, LogicLevel> levels = new LinkedHashMap<>();
        pieces.forEach(piece -> levels.putAll(piece.levels()));
        return levels;
    }

    /**
     * Keeps no readings — no part's energy, no peaks or means —: for a caller that reads the solution and not a screen,
     * a computed transient or a benchmark of the engine. The bench keeps them, and pays for them.
     */
    public LiveCircuitSolver solutionOnly() {
        pieces.forEach(LiveIntegrator::solutionOnly);
        return this;
    }

    /**
     * The circuit with new values, going on from where {@code previous} is — every piece from its own state.
     *
     * @throws IllegalArgumentException when it cannot go on: its pieces are not the ones it had
     */
    public static LiveCircuitSolver continuing(CompiledObject object, LiveCircuitSolver previous) {
        return continuing(object, previous, previous.interval);
    }

    /** The same circuit going on from where {@code previous} is, sampled every {@code sampleInterval} from now on. */
    public static LiveCircuitSolver continuing(CompiledObject object, LiveCircuitSolver previous, double sampleInterval) {
        ElaboratedCircuit elaborated = ElaboratedCircuit.of(object);
        List<ElaboratedCircuit> split = CircuitPartitions.of(elaborated);
        if (split.size() != previous.pieces.size()) {
            throw new IllegalArgumentException("The circuit changed its parts; it starts again.");
        }
        List<LiveIntegrator> continued = new ArrayList<>();
        for (int index = 0; index < split.size(); index++) {
            continued.add(LiveIntegrator.continuing(split.get(index), previous.pieces.get(index), sampleInterval));
        }
        LiveCircuitSolver solver = new LiveCircuitSolver(continued, sampleInterval, elaborated);
        solver.keepFailedFrom(previous);
        solver.warmedAt = previous.warmedAt;
        return solver;
    }

    /**
     * This solver — the same circuit built anew, from its start — with every part as warm as it was in {@code previous},
     * and the parts that failed since cooling from where they were: what a run does when a failure changed its parts.
     */
    public LiveCircuitSolver carryingHeat(LiveCircuitSolver previous) {
        Map<String, PartHeat> before = previous.heats();
        heats().forEach((part, heat) -> heat.continueFrom(before.get(part)));
        keepFailedFrom(previous);
        return this;
    }

    /** The failed parts' heat and limits, taken over from {@code previous}: their own devices are gone. */
    private void keepFailedFrom(LiveCircuitSolver previous) {
        Map<String, PartHeat> before = previous.heats();
        for (String part : failures.keySet()) {
            PartHeat heat = previous.cooling.containsKey(part) ? previous.cooling.get(part) : before.get(part);
            if (heat != null) {
                cooling.put(part, heat);
            }
            if (!limits.containsKey(part) && previous.limits.containsKey(part)) {
                limits.put(part, previous.limits.get(part));
            }
        }
    }

    /** The heat of every part that warms, by part — the hottest unit's for a part of several. */
    private Map<String, PartHeat> heats() {
        Map<String, PartHeat> heats = new LinkedHashMap<>();
        pieces.forEach(piece -> piece.heats().forEach(heats::putIfAbsent));
        return heats;
    }

    /** Each part's states as the last {@link #loads()} found them, by part and state name; a part without states is absent. */
    public synchronized Map<String, Map<String, Double>> states() {
        return states;
    }

    /**
     * How hard every part worked since this was last asked — its average power, its current now, how warm it is —, by
     * part. A part the cut split between two pieces (a supply both use) adds up.
     */
    public synchronized Map<String, PartLoad> loads() {
        Map<String, double[]> merged = new LinkedHashMap<>();
        Map<String, Map<String, Double>> terminals = new LinkedHashMap<>();
        Map<String, Map<String, Double>> partStates = new LinkedHashMap<>();
        for (LiveIntegrator piece : pieces) {
            for (LiveIntegrator.PartReading reading : piece.readParts()) {
                if (!reading.states().isEmpty()) {
                    partStates.computeIfAbsent(reading.part(), ignored -> new LinkedHashMap<>()).putAll(reading.states());
                }
                double[] sum = merged.computeIfAbsent(reading.part(), ignored -> new double[] {0, Double.NaN});
                sum[0] += reading.power();
                sum[1] = hotter(sum[1], reading.temperature());
                Map<String, Double> into = terminals.computeIfAbsent(reading.part(), ignored -> new LinkedHashMap<>());
                reading.terminals().forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
            }
        }
        // A part made of other parts works as hard as they do together, and its pins carry what crosses its boundary.
        for (ComponentHierarchy.Assembly assembly : assemblies) {
            double[] sum = merged.computeIfAbsent(assembly.part(), ignored -> new double[] {0, Double.NaN});
            assembly.members().keySet().forEach(member -> {
                double[] own = merged.get(member);
                if (own != null) {
                    sum[0] += own[0];
                    sum[1] = hotter(sum[1], own[1]);
                }
            });
            terminals.put(assembly.part(), assembly.pinCurrents(terminals));
        }
        double now = time();
        double seconds = now - warmedAt;
        warmedAt = now;
        Map<String, PartLoad> loads = new LinkedHashMap<>();
        merged.forEach((name, sum) -> {
            // As warm as its own heat law makes it — the engine keeps it, and on a realistic bench its equations read it.
            PartLimits limit = limits.get(name);
            loads.put(name, new PartLoad(sum[0], terminals.get(name), sum[1],
                    limit == null ? Double.NaN : limit.maximum(), limit == null ? Double.NaN : limit.rating(), null));
        });
        // A failed part takes nothing and cools by its own law; it is still there, burnt.
        failures.forEach((name, failure) -> {
            PartHeat heat = cooling.get(name);
            double temperature = Double.NaN;
            if (heat != null) {
                heat.advance(0, seconds);
                temperature = PhysicalConstants.celsius(heat.temperature());
            }
            PartLimits limit = limits.get(name);
            loads.put(name, new PartLoad(0, Map.of(), temperature, limit == null ? Double.NaN : limit.maximum(),
                    limit == null ? Double.NaN : limit.rating(), failure.kind().keyword()));
        });
        states = Map.copyOf(partStates);
        return loads;
    }

    /** The higher of two temperatures, NaN only where both are. */
    private static double hotter(double one, double other) {
        return Double.isNaN(one) ? other : Double.isNaN(other) ? one : Math.max(one, other);
    }

    /** How each part fails when it overheats, as its component writes it; {@code open} for a part that does not say. */
    public BenchConditions.Failure failureOf(String part) {
        PartLimits limit = limits.get(part);
        return limit == null ? BenchConditions.Failure.OPEN : limit.failure();
    }

    /** The circuit as it is simulated. */
    ElaboratedCircuit elaborated() {
        return elaborated;
    }

    /** The node names, in the order a sample reports them. */
    public List<String> nodes() {
        return nodes;
    }

    public double interval() {
        return interval;
    }

    public synchronized double time() {
        return pieces.getFirst().time();
    }

    /** The next {@code count} samples, the first at the time the last call ended. */
    public synchronized List<LiveSample> advance(int count) {
        List<List<LiveSample>> byPiece = new ArrayList<>();
        for (LiveIntegrator piece : pieces) {
            byPiece.add(piece.advance(count));
        }
        return merged(byPiece, count);
    }

    /**
     * Goes through the next {@code count} samples and returns only the traced ones: the circuit, its energy and its
     * trace go on, the samples no screen keeps are never read. Every piece traces the same samples, counted from the
     * start, so they line up.
     */
    public synchronized List<LiveSample> pass(long count) {
        List<List<LiveSample>> byPiece = new ArrayList<>();
        for (LiveIntegrator piece : pieces) {
            byPiece.add(piece.pass(count));
        }
        return merged(byPiece, byPiece.getFirst().size());
    }

    /** The pieces' samples, index by index, as the whole circuit's. */
    private List<LiveSample> merged(List<List<LiveSample>> byPiece, int count) {
        if (byPiece.size() == 1) {
            if (assemblies.isEmpty() && differenceOwners.length == 0) {
                return byPiece.getFirst();
            }
            return byPiece.getFirst().stream()
                    .map(sample -> new LiveSample(sample.time(), sample.voltages(), assembled(sample.terminals()), sample.metered(),
                            assembled(sample.window()), withDifferences(sample.envelope()), sample.powers())).toList();
        }
        List<LiveSample> merged = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double[] voltages = new double[nodes.size()];
            Map<String, Map<String, Double>> terminals = null;
            Map<String, MeteredPart> metered = null;
            Map<String, Double> powers = null;
            for (int piece = 0; piece < byPiece.size(); piece++) {
                LiveSample own = byPiece.get(piece).get(index);
                for (int node = 0; node < own.voltages().length; node++) {
                    voltages[placement[piece][node]] = own.voltages()[node];
                }
                // Every piece traces the same samples; a part the cut split between two pieces adds up, as in loads().
                if (own.terminals() != null) {
                    terminals = terminals == null ? new LinkedHashMap<>() : terminals;
                    for (Map.Entry<String, Map<String, Double>> reading : own.terminals().entrySet()) {
                        Map<String, Double> into = terminals.computeIfAbsent(reading.getKey(), ignored -> new LinkedHashMap<>());
                        reading.getValue().forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
                    }
                }
                if (own.powers() != null) {
                    powers = powers == null ? new LinkedHashMap<>() : powers;
                    Map<String, Double> into = powers;
                    own.powers().forEach((name, power) -> into.merge(name, power, Double::sum));
                }
                if (own.metered() != null) {
                    metered = metered == null ? new LinkedHashMap<>() : metered;
                    // A part the cut split between two pieces adds up, as its terminals and its power do in loads().
                    for (Map.Entry<String, MeteredPart> reading : own.metered().entrySet()) {
                        metered.merge(reading.getKey(), reading.getValue(), MeteredPart::plus);
                    }
                }
            }
            merged.add(new LiveSample(byPiece.getFirst().get(index).time(), voltages, assembled(terminals), metered,
                    assembled(mergedWindow(byPiece, index)), mergedEnvelope(byPiece, index), powers));
        }
        return merged;
    }

    /** The pieces' peaks at one sample as the whole circuit's, each node where its piece has it; none unless every piece read them. */
    private SampleEnvelope mergedEnvelope(List<List<LiveSample>> byPiece, int index) {
        double[] lows = new double[nodes.size()];
        double[] highs = new double[nodes.size()];
        double[] means = new double[nodes.size()];
        List<SampleEnvelope> envelopes = new ArrayList<>();
        for (int piece = 0; piece < byPiece.size(); piece++) {
            SampleEnvelope own = byPiece.get(piece).get(index).envelope();
            if (own == null) {
                return null;
            }
            envelopes.add(own);
            for (int node = 0; node < own.lows().length; node++) {
                lows[placement[piece][node]] = own.lows()[node];
                highs[placement[piece][node]] = own.highs()[node];
                means[placement[piece][node]] = own.means()[node];
            }
        }
        double[][] differences = mergedDifferences(envelopes);
        return new SampleEnvelope(lows, highs, means, differences[0], differences[1]);
    }

    /**
     * The pieces' windows at one sample as the whole circuit's: each node's mean and RMS where the piece has it, a part
     * the cut split adding up — its RMS only roughly, as two currents' RMS do not add. None unless every piece has one.
     */
    private SampleWindow mergedWindow(List<List<LiveSample>> byPiece, int index) {
        double[] means = new double[nodes.size()];
        double[] rms = new double[nodes.size()];
        Map<String, Map<String, Double>> terminalRms = new LinkedHashMap<>();
        for (int piece = 0; piece < byPiece.size(); piece++) {
            SampleWindow own = byPiece.get(piece).get(index).window();
            if (own == null) {
                return null;
            }
            for (int node = 0; node < own.means().length; node++) {
                means[placement[piece][node]] = own.means()[node];
                rms[placement[piece][node]] = own.rms()[node];
            }
            own.terminalRms().forEach((name, currents) -> {
                Map<String, Double> into = terminalRms.computeIfAbsent(name, ignored -> new LinkedHashMap<>());
                currents.forEach((terminal, current) -> into.merge(terminal, current, Double::sum));
            });
        }
        return new SampleWindow(means, rms, terminalRms);
    }

    /** A window with every part made of other parts reporting its pins' RMS as well — roughly, as {@link #mergedWindow} adds. */
    private SampleWindow assembled(SampleWindow window) {
        return window == null ? null : new SampleWindow(window.means(), window.rms(), assembled(window.terminalRms()));
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
        pieces.forEach(piece -> piece.meter(metered));
    }

    /**
     * Keeps the peaks of these differences of two nodes — positive and negative, as indices into {@link #nodes()},
     * {@link NodeDifference#GROUND} for ground — over every step, as each node's own are kept. A difference is kept by
     * the piece both its nodes are in; one whose nodes lie in two pieces has none (NaN in
     * {@link SampleEnvelope#differenceLows}).
     */
    public synchronized void difference(List<NodeDifference> pairs) {
        differenceOwners = new DifferenceOwner[pairs.size()];
        List<List<NodeDifference>> byPiece = new ArrayList<>();
        pieces.forEach(piece -> byPiece.add(new ArrayList<>()));
        for (int pair = 0; pair < pairs.size(); pair++) {
            for (int piece = 0; piece < pieces.size(); piece++) {
                int positive = localIndex(piece, pairs.get(pair).positive());
                int negative = localIndex(piece, pairs.get(pair).negative());
                if (positive != ABSENT && negative != ABSENT && !(positive == NodeDifference.GROUND && negative == NodeDifference.GROUND)) {
                    differenceOwners[pair] = new DifferenceOwner(piece, byPiece.get(piece).size());
                    byPiece.get(piece).add(new NodeDifference(positive, negative));
                    break;
                }
            }
        }
        for (int piece = 0; piece < pieces.size(); piece++) {
            pieces.get(piece).difference(byPiece.get(piece));
        }
    }

    /** A node of the whole circuit as a piece numbers it: ground as ground, {@link #ABSENT} when the piece does not have it. */
    private int localIndex(int piece, int node) {
        if (node == NodeDifference.GROUND) {
            return NodeDifference.GROUND;
        }
        for (int local = 0; local < placement[piece].length; local++) {
            if (placement[piece][local] == node) {
                return local;
            }
        }
        return ABSENT;
    }

    /** A single piece's envelope with its differences in the order watched — a pair it could not keep is missing from its own. */
    private SampleEnvelope withDifferences(SampleEnvelope own) {
        if (own == null || differenceOwners.length == 0) {
            return own;
        }
        double[][] differences = mergedDifferences(List.of(own));
        return new SampleEnvelope(own.lows(), own.highs(), own.means(), differences[0], differences[1]);
    }

    /** The watched differences' peaks at one sample, in the order watched, from the pieces that keep them. */
    private double[][] mergedDifferences(List<SampleEnvelope> envelopes) {
        double[] lows = new double[differenceOwners.length];
        double[] highs = new double[differenceOwners.length];
        for (int pair = 0; pair < differenceOwners.length; pair++) {
            DifferenceOwner owner = differenceOwners[pair];
            SampleEnvelope own = owner == null ? null : envelopes.get(owner.piece());
            boolean kept = own != null && owner.place() < own.differenceLows().length;
            lows[pair] = kept ? own.differenceLows()[owner.place()] : Double.NaN;
            highs[pair] = kept ? own.differenceHighs()[owner.place()] : Double.NaN;
        }
        return new double[][]{lows, highs};
    }

    /** Reads every part's terminal currents on every {@code every}-th sample as well; 0 reads none. */
    public synchronized void trace(int every) {
        pieces.forEach(piece -> piece.trace(every));
    }

    /** Traces only these parts' terminal currents; null traces every part — what a sheet that draws them all needs. */
    public synchronized void traceOnly(Set<String> only) {
        pieces.forEach(piece -> piece.traceOnly(only));
    }
}
