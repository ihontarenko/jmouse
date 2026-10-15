package org.jmouse.parallax.circuit.benchmark;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.circuit.CircuitComputations;
import org.jmouse.parallax.circuit.LiveCircuitSession;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.spice.SpiceConverter;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceValue;
import org.jmouse.spice.parser.SpiceParser;
import org.jmouse.spice.printer.SpicePrinter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Our engine against ngspice, circuit by circuit: each fixed benchmark circuit is written as SPICE by the converter
 * ({@link SpiceConverter}), run by ngspice in batch mode, and run here twice — the batch transient
 * ({@link CircuitComputations.TransientResponse}, what a computed graph runs) and the live engine
 * ({@link LiveCircuitSession}, as {@link LiveEngineBenchmark} runs it) —; the table says how long each took and whether
 * their answers agree: each probed node's final value and its mean over the second half of the run, and its frequency
 * and duty when it is periodic. A line that disagrees beyond {@link #TOLERANCE} says {@code FAIL}.
 *
 * <p>⚠️ Times are paired, never compared across runs: this machine drifts 15–25 % within a session, so ngspice and the
 * batch engine run alternately, round after round, and each time is the median of its rounds. ngspice's time is its
 * process, start-up included, and — in parentheses — the analysis time it reports itself.</p>
 *
 * <p>A circuit whose parts SPICE cannot describe — a timer or an amplifier with a state or behaviour of its own — is
 * reported as not comparable, with the converter's note saying why, rather than compared without those parts.</p>
 *
 * <pre>
 * java -Dngspice=…\Spice64\bin\ngspice_con.exe -cp &lt;test classpath&gt; \
 *     org.jmouse.parallax.circuit.benchmark.SpiceComparisonBenchmark [rounds [circuit …]]
 * </pre>
 */
public final class SpiceComparisonBenchmark {

    /** How far two answers may be apart, as a share of the node's swing (or of its level, for a node that does not swing). */
    static final double TOLERANCE = 0.02;
    /** How far two duties may be apart, absolutely. */
    static final double DUTY_TOLERANCE = 0.01;
    private static final int BATCH_SAMPLES = 4001;
    private static final int LIVE_SAMPLES = 4000;
    private static final int SAMPLES_PER_PIECE = 64;

    /**
     * A circuit, the nodes whose signals are compared, and how many steps ngspice is held to at most over the span.
     * A switching edge shorter than one sample needs a finer reference: at one step per sample ngspice strides over a
     * transistor's turn-off and rings past the rail (10.8 V on a 10 V supply; 10.34 V at 2 ns steps, 10.31 V here).
     */
    record Case(String circuit, List<String> nodes, int referenceSteps) {

        Case(String circuit, List<String> nodes) {
            this(circuit, nodes, LIVE_SAMPLES);
        }
    }

    static final List<Case> CASES = List.of(
            new Case("rc-low-pass", List.of("out")),
            new Case("rlc-band-pass", List.of("out", "middle")),
            new Case("bridge-rectifier-resistive", List.of("p")),
            new Case("bridge-rectifier", List.of("p")),
            new Case("square-rc", List.of("in", "out", "clamp")),
            new Case("pwm-filter-lab", List.of("pwm", "out")),
            new Case("pwm-blocks-4", List.of("pwm_1", "buffered_1")),
            // Coupled end to end: one piece, nothing for the partitioning to cut — the case it cannot help.
            new Case("rc-ladder-40", List.of("n20", "n40")),
            new Case("rc-ladder-200", List.of("n100", "n200")),
            new Case("voltage-multiplier-4", List.of("b2", "b4")),
            new Case("voltage-multiplier-12", List.of("b6", "b12")),
            new Case("npn-switch", List.of("base", "collector"), 50_000),
            new Case("pnp-switch", List.of("base", "collector"), 50_000),
            new Case("nmos-switch", List.of("gate", "drain"), 50_000),
            new Case("pmos-switch", List.of("gate", "drain"), 50_000));

    /** What ngspice's {@code rusage trantime} prints: the transient's own time, without the process starting. */
    private static final Pattern ANALYSIS_TIME = Pattern.compile("Transient analysis time = ([0-9.eE+-]+)");

    private SpiceComparisonBenchmark() {
    }

    public static void main(String[] arguments) throws IOException, InterruptedException {
        String ngspice = System.getProperty("ngspice");
        if (ngspice == null || !Files.isRegularFile(Path.of(ngspice))) {
            System.out.println("SKIPPED: ngspice is not given — run with -Dngspice=<path to ngspice_con.exe>"
                    + (ngspice == null ? "" : "; '" + ngspice + "' is not a file"));
            return;
        }
        int rounds = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 3;
        // Circuits named after the rounds run alone; none named runs every case.
        List<String> named = List.of(arguments).subList(Math.min(1, arguments.length), arguments.length);
        ComponentLibrary library = reference -> resource("components/" + reference + ".jmx");
        SpiceConverter converter = new SpiceConverter(library);
        Path directory = Files.createTempDirectory("spice-comparison");
        System.out.printf("%-27s %-10s %20s %10s %10s  %-26s %-26s %-26s %-29s %-17s %s%n", "circuit", "node", "ngspice ms (own)",
                "batch ms", "live ms", "final  spice/batch/live", "mean   spice/batch/live", "swing  spice/batch/live",
                "f, Hz  spice/batch/live",
                "duty s/b/l", "verdict");
        int failures = 0;
        for (Case benchmark : CASES) {
            if (!named.isEmpty() && !named.contains(benchmark.circuit())) {
                continue;
            }
            failures += compare(benchmark, converter, library, Path.of(ngspice), directory, rounds);
        }
        System.out.println(failures == 0 ? "ALL AGREE within " + (int) (TOLERANCE * 100) + " %"
                : failures + " FAIL line(s): the engines disagree beyond " + (int) (TOLERANCE * 100) + " %");
    }

    // ─── one circuit ────────────────────────────────────────────────────────────────────────────────

    private static int compare(Case benchmark, SpiceConverter converter, ComponentLibrary library, Path ngspice,
                               Path directory, int rounds) throws IOException, InterruptedException {
        String source = resource("circuits/" + benchmark.circuit() + ".jmx").orElseThrow();
        SpiceConverter.SpiceExport exported = converter.exportDocument(source);
        // A part the converter had to leave out makes the deck another circuit: comparing it would say nothing.
        Optional<String> incomparable = exported.notes().stream()
                .filter(note -> note.contains("cannot be a SPICE subcircuit") || note.contains("is not in the library")
                        || note.contains("does not read") || note.contains("connected to nothing"))
                .findFirst();
        if (incomparable.isPresent()) {
            System.out.printf("%-27s NOT COMPARABLE — %s%n", benchmark.circuit(), incomparable.get());
            return 0;
        }
        CompiledObject compiled = compile(source, library);
        double span = ((org.jmouse.parallax.object.ObjectDefinition.Circuit) compiled.definition()).time().maximum();
        Path deckFile = directory.resolve(benchmark.circuit() + ".cir");
        Path dataFile = directory.resolve(benchmark.circuit() + ".data");
        Files.writeString(deckFile, withOutput(exported.text(), dataFile.getFileName().toString(), benchmark.nodes(), span, benchmark.referenceSteps()));

        runBatch(compiled);
        List<Double> ngspiceTimes = new ArrayList<>();
        List<Double> ngspiceOwnTimes = new ArrayList<>();
        List<Double> batchTimes = new ArrayList<>();
        Dataset.TimeSeries batch = null;
        for (int round = 0; round < rounds; round++) {
            boolean ngspiceFirst = round % 2 == 0;
            if (!ngspiceFirst) {
                long start = System.nanoTime();
                batch = runBatch(compiled);
                batchTimes.add((System.nanoTime() - start) / 1e6);
            }
            long start = System.nanoTime();
            double own = runNgspice(ngspice, deckFile, directory);
            ngspiceTimes.add((System.nanoTime() - start) / 1e6);
            ngspiceOwnTimes.add(own * 1000);
            if (ngspiceFirst) {
                long batchStart = System.nanoTime();
                batch = runBatch(compiled);
                batchTimes.add((System.nanoTime() - batchStart) / 1e6);
            }
        }
        long liveStart = System.nanoTime();
        Map<String, Signal> live = runLive(compiled, span, benchmark.nodes());
        double liveTime = (System.nanoTime() - liveStart) / 1e6;
        Map<String, Signal> spice = readData(dataFile, benchmark.nodes());

        int failures = 0;
        boolean first = true;
        for (String node : benchmark.nodes()) {
            // A reference finer than our samples is read at our samples: a spike between two of them is seen by neither.
            Signal reference = benchmark.referenceSteps() > LIVE_SAMPLES ? spice.get(node).at(live.get(node).time()) : spice.get(node);
            Measures fromSpice = reference.measures(span);
            Measures fromBatch = Signal.of(batch, node).measures(span);
            Measures fromLive = live.get(node).measures(span);
            List<String> disagreements = new ArrayList<>();
            disagreements.addAll(fromBatch.disagreements(fromSpice, "batch"));
            disagreements.addAll(fromLive.disagreements(fromSpice, "live"));
            failures += disagreements.isEmpty() ? 0 : 1;
            System.out.printf(Locale.ROOT, "%-27s %-10s %20s %10s %10s  %-26s %-26s %-26s %-29s %-17s %s%n",
                    first ? benchmark.circuit() : "", node,
                    first ? String.format(Locale.ROOT, "%.1f (%.1f)", median(ngspiceTimes), median(ngspiceOwnTimes)) : "",
                    first ? String.format(Locale.ROOT, "%.1f", median(batchTimes)) : "",
                    first ? String.format(Locale.ROOT, "%.1f", liveTime) : "",
                    triple(fromSpice.last(), fromBatch.last(), fromLive.last()),
                    triple(fromSpice.mean(), fromBatch.mean(), fromLive.mean()),
                    triple(fromSpice.swing(), fromBatch.swing(), fromLive.swing()),
                    triple(fromSpice.frequency(), fromBatch.frequency(), fromLive.frequency()),
                    tripleShort(fromSpice.duty(), fromBatch.duty(), fromLive.duty()),
                    disagreements.isEmpty() ? "agree" : "FAIL " + String.join("; ", disagreements));
            first = false;
        }
        return failures;
    }

    /**
     * The exported deck, built on as nodes: its transient told to step no longer than the live engine's sample, so both
     * are read at the same resolution, and a {@code .control} script that runs it, reports its time and writes the nodes
     * compared.
     */
    private static String withOutput(String exported, String dataFile, List<String> nodes, double span, int steps) {
        SpiceDeck deck = new SpiceParser().parse(exported);
        List<org.jmouse.el.node.Node> children = deck.getChildren();
        for (int index = 0; index < children.size(); index++) {
            if (children.get(index) instanceof SpiceCard.Transient analysis) {
                SpiceCard.Transient bounded = new SpiceCard.Transient(analysis.step(), analysis.stop(),
                        analysis.start().orElse(SpiceValue.number(0)), SpiceValue.number(span / steps), false,
                        SpicePosition.NONE);
                bounded.setParent(deck);
                children.set(index, bounded);
            }
        }
        List<String> vectors = nodes.stream().map(node -> "v(" + node + ")").toList();
        SpiceCard.Control control = new SpiceCard.Control(List.of("run", "rusage trantime",
                "wrdata " + dataFile + " " + String.join(" ", vectors), "quit"), SpicePosition.NONE);
        int end = children.size();
        if (end > 0 && children.getLast() instanceof SpiceCard.End) {
            end--;
        }
        children.add(end, control);
        control.setParent(deck);
        return SpicePrinter.print(deck);
    }

    // ─── the three runs ─────────────────────────────────────────────────────────────────────────────

    private static double runNgspice(Path ngspice, Path deck, Path directory) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(ngspice.toString(), "-b", deck.getFileName().toString())
                .directory(directory.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0 || output.contains("Error")) {
            throw new IllegalStateException("ngspice refused " + deck + " (exit " + exit + "):\n" + output);
        }
        Matcher analysis = ANALYSIS_TIME.matcher(output);
        return analysis.find() ? Double.parseDouble(analysis.group(1)) : Double.NaN;
    }

    private static Dataset.TimeSeries runBatch(CompiledObject compiled) {
        CircuitComputations.TransientResponse transientResponse = new CircuitComputations.TransientResponse();
        ComputationSettings settings = ComputationSettings.resolve(transientResponse.settings(),
                Map.of("samples", BATCH_SAMPLES));
        return (Dataset.TimeSeries) transientResponse.compute(compiled, settings, ComputationProgress.NONE);
    }

    private static Map<String, Signal> runLive(CompiledObject compiled, double span, List<String> nodes) {
        double interval = span / LIVE_SAMPLES;
        LiveCircuitSession session = new LiveCircuitSession(compiled, interval, nodes.stream()
                .map(node -> new LiveCircuitSession.Probe(node, node, "0")).toList());
        List<double[]> times = new ArrayList<>();
        Map<String, List<double[]>> readings = new java.util.LinkedHashMap<>();
        nodes.forEach(node -> readings.put(node, new ArrayList<>()));
        double reached = 0;
        while (reached < span) {
            LiveCircuitSession.Frame frame = session.advance(SAMPLES_PER_PIECE);
            if (frame.time().length == 0) {
                break;
            }
            times.add(frame.time());
            nodes.forEach(node -> readings.get(node).add(frame.channels().get(node)));
            reached = frame.time()[frame.time().length - 1];
        }
        double[] time = times.stream().flatMapToDouble(Arrays::stream).toArray();
        Map<String, Signal> signals = new java.util.LinkedHashMap<>();
        readings.forEach((node, pieces) -> signals.put(node, new Signal(time,
                pieces.stream().flatMapToDouble(Arrays::stream).toArray()).until(span)));
        return signals;
    }

    /** {@code wrdata}'s columns: each vector as its time and its value, side by side. */
    private static Map<String, Signal> readData(Path file, List<String> nodes) throws IOException {
        List<double[]> rows = Files.readAllLines(file).stream().map(String::strip).filter(line -> !line.isEmpty())
                .map(line -> Arrays.stream(line.split("\\s+")).mapToDouble(Double::parseDouble).toArray()).toList();
        Map<String, Signal> signals = new java.util.LinkedHashMap<>();
        for (int index = 0; index < nodes.size(); index++) {
            int column = index;
            signals.put(nodes.get(index), new Signal(rows.stream().mapToDouble(row -> row[2 * column]).toArray(),
                    rows.stream().mapToDouble(row -> row[2 * column + 1]).toArray()));
        }
        return signals;
    }

    // ─── what is compared ───────────────────────────────────────────────────────────────────────────

    /** A node's voltage over time, at whatever instants the simulator chose. */
    record Signal(double[] time, double[] value) {

        static Signal of(Dataset.TimeSeries series, String node) {
            int column = series.columns().indexOf(node);
            if (column < 0) {
                throw new IllegalStateException("The batch run has no column " + node + ": " + series.columns());
            }
            float[] axis = series.axisValues();
            float[] values = series.values().get(column);
            double[] time = new double[axis.length];
            double[] value = new double[axis.length];
            for (int sample = 0; sample < axis.length; sample++) {
                time[sample] = axis[sample];
                value[sample] = values[sample];
            }
            return new Signal(time, value);
        }

        /** The signal at the given instants, by linear interpolation between its own. */
        Signal at(double[] instants) {
            double[] values = new double[instants.length];
            int next = 0;
            for (int index = 0; index < instants.length; index++) {
                while (next < time.length - 1 && time[next + 1] < instants[index]) {
                    next++;
                }
                if (next == time.length - 1) {
                    values[index] = value[next];
                    continue;
                }
                double share = (instants[index] - time[next]) / (time[next + 1] - time[next]);
                values[index] = value[next] + Math.max(0, Math.min(1, share)) * (value[next + 1] - value[next]);
            }
            return new Signal(instants.clone(), values);
        }

        /** The signal up to {@code end}, its last value there by interpolation: a run that went past it is cut back. */
        Signal until(double end) {
            int count = 0;
            while (count < time.length && time[count] <= end) {
                count++;
            }
            if (count == time.length || count == 0) {
                return this;
            }
            double[] cutTime = Arrays.copyOf(time, count + 1);
            double[] cutValue = Arrays.copyOf(value, count + 1);
            double share = (end - time[count - 1]) / (time[count] - time[count - 1]);
            cutTime[count] = end;
            cutValue[count] = value[count - 1] + share * (value[count] - value[count - 1]);
            return new Signal(cutTime, cutValue);
        }

        /**
         * What is compared, over the second half of the run — after whatever the start set going has settled: the mean
         * (weighted by time, since a simulator's instants are uneven), the swing, and, for a periodic node, its frequency
         * and its duty from crossings of the middle level found by interpolation.
         */
        Measures measures(double span) {
            double from = span / 2;
            double weighted = 0;
            double covered = 0;
            double low = Double.POSITIVE_INFINITY;
            double high = Double.NEGATIVE_INFINITY;
            for (int sample = 1; sample < time.length; sample++) {
                if (time[sample] <= from) {
                    continue;
                }
                double start = Math.max(time[sample - 1], from);
                double width = time[sample] - start;
                weighted += width * (value[sample] + value[sample - 1]) / 2;
                covered += width;
                low = Math.min(low, value[sample]);
                high = Math.max(high, value[sample]);
            }
            double mean = covered > 0 ? weighted / covered : value[value.length - 1];
            double level = (low + high) / 2;
            List<Double> rising = new ArrayList<>();
            List<Double> falling = new ArrayList<>();
            for (int sample = 1; sample < time.length; sample++) {
                if (time[sample - 1] < from) {
                    continue;
                }
                boolean up = value[sample - 1] < level && value[sample] >= level;
                boolean down = value[sample - 1] >= level && value[sample] < level;
                if (up || down) {
                    double share = (level - value[sample - 1]) / (value[sample] - value[sample - 1]);
                    (up ? rising : falling).add(time[sample - 1] + share * (time[sample] - time[sample - 1]));
                }
            }
            boolean swings = high - low > 0.05 * Math.max(Math.abs(high), Math.abs(low));
            double frequency = Double.NaN;
            double duty = Double.NaN;
            if (swings && rising.size() >= 3) {
                double first = rising.getFirst();
                double last = rising.getLast();
                double period = (last - first) / (rising.size() - 1);
                frequency = 1 / period;
                double highTime = 0;
                int pulses = 0;
                for (double rise : rising) {
                    Optional<Double> fall = falling.stream().filter(candidate -> candidate > rise).findFirst();
                    if (fall.isPresent() && fall.get() - rise < period) {
                        highTime += fall.get() - rise;
                        pulses++;
                    }
                }
                duty = pulses == 0 ? Double.NaN : highTime / pulses / period;
            }
            return new Measures(value[value.length - 1], mean, high - low, Math.max(Math.abs(high), Math.abs(low)),
                    frequency, duty);
        }
    }

    /**
     * @param swing     the highest less the lowest over the second half
     * @param magnitude the largest absolute value there
     */
    record Measures(double last, double mean, double swing, double magnitude, double frequency, double duty) {

        boolean periodic() {
            return !Double.isNaN(frequency);
        }

        /**
         * What disagrees with ngspice's answer, in words; nothing when everything agrees. A periodic node's last value
         * is a phase — two simulators that land their last instant a hair apart read different points of the same wave
         * — so its swing is compared instead.
         */
        List<String> disagreements(Measures spice, String engine) {
            List<String> found = new ArrayList<>();
            double scale = Math.max(Math.max(spice.swing, spice.magnitude), 1e-3);
            if (!spice.periodic() && Math.abs(last - spice.last) > TOLERANCE * scale) {
                found.add(engine + " final " + format(last) + " vs " + format(spice.last));
            }
            if (Math.abs(mean - spice.mean) > TOLERANCE * scale) {
                found.add(engine + " mean " + format(mean) + " vs " + format(spice.mean));
            }
            if (Math.abs(swing - spice.swing) > TOLERANCE * scale) {
                found.add(engine + " swing " + format(swing) + " vs " + format(spice.swing));
            }
            if (periodic() != spice.periodic()) {
                found.add(engine + " periodic " + periodic() + " vs " + spice.periodic());
            } else if (periodic()) {
                if (Math.abs(frequency - spice.frequency) > TOLERANCE * spice.frequency) {
                    found.add(engine + " frequency " + format(frequency) + " vs " + format(spice.frequency));
                }
                if (Math.abs(duty - spice.duty) > DUTY_TOLERANCE) {
                    found.add(engine + " duty " + format(duty) + " vs " + format(spice.duty));
                }
            }
            return found;
        }
    }

    // ─── small things ───────────────────────────────────────────────────────────────────────────────

    private static CompiledObject compile(String source, ComponentLibrary library) {
        DefinedObject defined = ExperienceDefinitions.read(source, "benchmark");
        return DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(), library);
    }

    private static String triple(double spice, double batch, double live) {
        return format(spice) + " / " + format(batch) + " / " + format(live);
    }

    private static String tripleShort(double spice, double batch, double live) {
        return Double.isNaN(spice) && Double.isNaN(batch) && Double.isNaN(live) ? "—"
                : String.format(Locale.ROOT, "%.3f/%.3f/%.3f", spice, batch, live);
    }

    private static String format(double value) {
        return Double.isNaN(value) ? "—" : String.format(Locale.ROOT, "%.4g", value);
    }

    private static double median(List<Double> values) {
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return sorted.length == 0 ? Double.NaN : sorted[sorted.length / 2];
    }

    private static Optional<String> resource(String path) {
        try (InputStream stream = SpiceComparisonBenchmark.class.getResourceAsStream("/circuit-benchmark/" + path)) {
            return stream == null ? Optional.empty() : Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
