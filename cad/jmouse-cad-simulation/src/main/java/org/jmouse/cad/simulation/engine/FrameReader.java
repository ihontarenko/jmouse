package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.engine.LiveCircuitSession.Peaks;
import org.jmouse.cad.simulation.engine.LiveCircuitSession.Probe;
import org.jmouse.cad.simulation.engine.LiveCircuitSession.Quantity;
import org.jmouse.cad.simulation.engine.LiveCircuitSession.Trace;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a frame's samples read on the probes, and the circuit-wide readings beside them: each probe's channel — the
 * voltage between its two nodes, the current into a part's terminal, the power a part takes, what an instrument reads —,
 * each voltage channel's peaks, every node's mean level, and the trace. Pure reading: it changes nothing it is given.
 */
final class FrameReader {

    /**
     * What an instrument probe reads: one figure of its part's {@code measure} block, compiled for that part, and its unit.
     * The probe names the figure as its terminal; a probe naming none reads the instrument's first.
     */
    record InstrumentReading(PartExpression expression, String unit) {
    }

    /** A part's first terminal, which a current probe naming none reads: a primitive's "+". */
    private static final String POSITIVE = SimulationVocabulary.IN_ORDER_TERMINALS.getFirst();

    private final List<Probe>                    probes;
    private final Map<String, InstrumentReading> instruments;
    private final List<String>                   order;
    /** Each probe's two nodes, and an instrument probe's pins, among the samples' voltages; found once for the frame. */
    private final NodeReference[]                positives;
    private final NodeReference[]                negatives;
    private final NodeReference[][]              instrumentNodes;

    FrameReader(List<Probe> probes, Map<String, InstrumentReading> instruments, List<String> order) {
        this.probes = probes;
        this.instruments = instruments;
        this.order = order;
        this.positives = probes.stream().map(probe -> nodeOf(probe, probe.positive())).toArray(NodeReference[]::new);
        this.negatives = probes.stream().map(probe -> nodeOf(probe, probe.negative())).toArray(NodeReference[]::new);
        this.instrumentNodes = probes.stream().map(probe -> {
            InstrumentReading reading = instruments.get(probe.channel());
            return reading == null ? new NodeReference[0]
                    : reading.expression().pinNodes().stream().map(node -> NodeReference.of(order, node)).toArray(NodeReference[]::new);
        }).toArray(NodeReference[][]::new);
    }

    /**
     * Whether a probe's difference is one the solver keeps the peaks of: a voltage across a part — its reference a node,
     * not ground —, both nodes in the circuit.
     */
    static boolean watchesDifference(Probe probe, List<String> order) {
        return probe.quantity() == Quantity.VOLTAGE && NodeReference.of(order, probe.negative()) instanceof NodeReference.Node
                && !(NodeReference.of(order, probe.positive()) instanceof NodeReference.Absent);
    }

    /** A voltage or logic probe's node among the samples' values; a probe on a part, or on a digital node, reads none. */
    private NodeReference nodeOf(Probe probe, String node) {
        boolean onNode = probe.quantity() == Quantity.VOLTAGE || probe.quantity() == Quantity.LEVEL;
        return onNode && node != null ? NodeReference.of(order, node) : NodeReference.ABSENT;
    }

    /** Whether a logic probe reads a digital node's level rather than an analog node's voltage. */
    private boolean readsLevel(int probe) {
        return probes.get(probe).quantity() == Quantity.LEVEL && positives[probe] instanceof NodeReference.Absent;
    }

    /** Each probe's unit: an instrument's own, or its quantity's. */
    Map<String, String> units() {
        Map<String, String> units = new LinkedHashMap<>();
        for (int index = 0; index < probes.size(); index++) {
            Probe probe = probes.get(index);
            InstrumentReading reading = instruments.get(probe.channel());
            String unit = switch (probe.quantity()) {
                case READING -> reading == null ? "" : reading.unit();
                // An analog node read by a logic channel arrives as its voltage, for the analyser's thresholds.
                case LEVEL -> readsLevel(index) ? Quantity.LEVEL.unit() : Quantity.VOLTAGE.unit();
                default -> probe.quantity().unit();
            };
            units.put(probe.channel(), unit);
        }
        return units;
    }

    /** Each probe's readings over these samples, by channel. */
    Map<String, double[]> channels(List<LiveSample> samples) {
        Map<String, double[]> channels = new LinkedHashMap<>();
        double[][] columns = new double[probes.size()][];
        for (int probe = 0; probe < probes.size(); probe++) {
            columns[probe] = new double[samples.size()];
            channels.put(probes.get(probe).channel(), columns[probe]);
        }
        for (int index = 0; index < samples.size(); index++) {
            LiveSample sample = samples.get(index);
            for (int probe = 0; probe < probes.size(); probe++) {
                columns[probe][index] = switch (probes.get(probe).quantity()) {
                    case VOLTAGE -> positives[probe].in(sample.voltages()) - negatives[probe].in(sample.voltages());
                    case CURRENT -> current(sample, probes.get(probe));
                    case POWER -> power(sample, probes.get(probe));
                    case READING -> reading(sample, probes.get(probe), instrumentNodes[probe]);
                    case LEVEL -> readsLevel(probe) ? level(sample, probes.get(probe).positive()) : positives[probe].in(sample.voltages());
                };
            }
        }
        return channels;
    }

    /** A digital node's level at a sample, as a dataset writes one: 1, 0, −1 unknown, −2 let go. */
    private static double level(LiveSample sample, String node) {
        LogicLevel level = sample.levels() == null ? null : sample.levels().get(node);
        return (level == null ? LogicLevel.UNKNOWN : level).datasetValue();
    }

    /** Every probe's channel, read on no samples: what a frame that passed its samples unread carries. */
    Map<String, double[]> emptyChannels() {
        Map<String, double[]> channels = new LinkedHashMap<>();
        probes.forEach(probe -> channels.put(probe.channel(), new double[0]));
        return channels;
    }

    /**
     * Each voltage channel's peaks over a sample's interval: against ground, its node's; across a part, the difference's
     * own, kept by the solver step by step ({@link #watchesDifference}). Only where the solver could not keep one — the
     * two nodes in separate pieces of the circuit — the lowest of the node against the highest of the reference stands
     * in: a bound, twice the swing on a slope where the two move apart, so never where it can be avoided.
     */
    Map<String, Peaks> peaks(List<LiveSample> samples) {
        Map<String, Peaks> peaks = new LinkedHashMap<>();
        if (samples.isEmpty() || samples.getFirst().envelope() == null) {
            return peaks;
        }
        int watched = 0;
        for (int probe = 0; probe < probes.size(); probe++) {
            if (probes.get(probe).quantity() != Quantity.VOLTAGE) {
                continue;
            }
            int difference = watchesDifference(probes.get(probe), order) ? watched++ : -1;
            NodeReference positive = positives[probe];
            NodeReference negative = negatives[probe];
            double[] lows = new double[samples.size()];
            double[] highs = new double[samples.size()];
            double[] means = new double[samples.size()];
            for (int index = 0; index < samples.size(); index++) {
                SampleEnvelope envelope = samples.get(index).envelope();
                if (envelope == null) {
                    double reading = positive.in(samples.get(index).voltages()) - negative.in(samples.get(index).voltages());
                    lows[index] = reading;
                    highs[index] = reading;
                    means[index] = reading;
                    continue;
                }
                boolean kept = difference >= 0 && difference < envelope.differenceLows().length
                        && Double.isFinite(envelope.differenceLows()[difference]);
                lows[index] = kept ? envelope.differenceLows()[difference] : positive.in(envelope.lows()) - negative.in(envelope.highs());
                highs[index] = kept ? envelope.differenceHighs()[difference] : positive.in(envelope.highs()) - negative.in(envelope.lows());
                // A mean is linear: the difference of the means is the mean of the difference, exactly.
                means[index] = positive.in(envelope.means()) - negative.in(envelope.means());
            }
            peaks.put(probes.get(probe).channel(), new Peaks(lows, highs, means));
        }
        return peaks;
    }

    /** Every node's mean voltage over these samples, by node: a traced sample's stretch where it stands for one. */
    Map<String, Double> meanLevels(List<LiveSample> samples) {
        Map<String, Double> levels = new LinkedHashMap<>();
        if (samples.isEmpty()) {
            return levels;
        }
        double[] sums = new double[order.size()];
        for (LiveSample sample : samples) {
            for (int node = 0; node < sums.length; node++) {
                sums[node] += sample.window() != null ? sample.window().means()[node] : sample.voltages()[node];
            }
        }
        for (int node = 0; node < sums.length; node++) {
            levels.put(order.get(node), sums[node] / samples.size());
        }
        return levels;
    }

    /** The traced samples among these as a trace: every node's voltage and every part terminal's current. */
    Trace trace(List<LiveSample> samples) {
        List<LiveSample> traced = samples.stream().filter(sample -> sample.terminals() != null).toList();
        if (traced.isEmpty()) {
            return Trace.EMPTY;
        }
        double[] time = new double[traced.size()];
        Map<String, double[]> columns = new LinkedHashMap<>();
        Map<String, double[]> rms = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (String node : order) {
            columns.put(node, new double[traced.size()]);
            rms.put(node, new double[traced.size()]);
            units.put(node, ColumnNames.VOLTS);
        }
        for (int index = 0; index < traced.size(); index++) {
            LiveSample sample = traced.get(index);
            SampleWindow window = sample.window();
            time[index] = sample.time();
            for (int node = 0; node < order.size(); node++) {
                double voltage = window != null ? window.means()[node] : sample.voltages()[node];
                columns.get(order.get(node))[index] = voltage;
                rms.get(order.get(node))[index] = window != null ? window.rms()[node] : Math.abs(voltage);
            }
            for (Map.Entry<String, Map<String, Double>> part : sample.terminals().entrySet()) {
                Map<String, Double> partRms = window != null ? window.terminalRms().getOrDefault(part.getKey(), Map.of()) : Map.of();
                for (Map.Entry<String, Double> terminal : part.getValue().entrySet()) {
                    String column = ColumnNames.partCurrent(part.getKey(), terminal.getKey());
                    if (column != null) {
                        double current = terminal.getValue();
                        columns.computeIfAbsent(column, ignored -> unknown(traced.size()))[index] = current;
                        rms.computeIfAbsent(column, ignored -> unknown(traced.size()))[index] = partRms.getOrDefault(terminal.getKey(), Math.abs(current));
                        units.put(column, ColumnNames.AMPERES);
                    }
                }
            }
        }
        return new Trace(time, columns, units, rms);
    }

    private static double[] unknown(int size) {
        double[] values = new double[size];
        Arrays.fill(values, Double.NaN);
        return values;
    }

    /** The current into the probe's terminal of its part — its first when it names none —; NaN for a part not metered. */
    private static double current(LiveSample sample, Probe probe) {
        MeteredPart metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        if (metered == null || metered.terminals() == null || metered.terminals().isEmpty()) {
            return Double.NaN;
        }
        // A primitive's first terminal is its "+": its terminals come in no fixed order, and the sign must not depend on it.
        String terminal = probe.terminal() != null ? probe.terminal()
                : metered.terminals().containsKey(POSITIVE) ? POSITIVE : metered.terminals().keySet().iterator().next();
        Double current = metered.terminals().get(terminal);
        return current == null ? Double.NaN : current;
    }

    private static double power(LiveSample sample, Probe probe) {
        MeteredPart metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        return metered == null ? Double.NaN : metered.power();
    }

    /** What an instrument reads at a sample, from the voltages of its pins and what its part is doing then. */
    private double reading(LiveSample sample, Probe probe, NodeReference[] pins) {
        InstrumentReading reading = instruments.get(probe.channel());
        if (reading == null) {
            return Double.NaN;
        }
        double[] voltages = new double[pins.length];
        for (int pin = 0; pin < pins.length; pin++) {
            voltages[pin] = pins[pin].in(sample.voltages());
        }
        MeteredPart metered = sample.metered() == null ? null : sample.metered().get(probe.part());
        return reading.expression().read(voltages, metered, sample.time());
    }
}
