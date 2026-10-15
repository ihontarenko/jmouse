package org.jmouse.parallax.circuit.benchmark;

import org.jmouse.parallax.circuit.LiveCircuitSession;
import org.jmouse.parallax.circuit.LiveCircuitSolver;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * How fast the live engine runs a circuit, measured the way a live bench runs it: a {@link LiveCircuitSession} advanced
 * 64 samples at a time, its probes measured and its parts' loads read after every piece — what {@code LiveRun} does on
 * the server, without the socket. With no arguments it prints the table of every circuit and tracing; with
 * {@code circuit every} it runs that one scenario and prints one {@code RESULT} line, which is how
 * {@link PairedEngineBenchmark} runs each in a JVM of its own.
 *
 * <p>⚠️ Two runs of it at different times compare nothing: this machine drifts 15–25 % within a session. A change to
 * the engine is judged by {@link PairedEngineBenchmark} — the old engine and the new one back to back, as a ratio.</p>
 *
 * <p>The circuits and their component models are copies under {@code circuit-benchmark/}, fixed on purpose: a benchmark
 * compares against its own past, so it never follows the catalogue.</p>
 */
public final class LiveEngineBenchmark {

    /** The oscilloscope's default: 1 ms a division, a thousand samples a screen. */
    static final double SAMPLE_INTERVAL = 1e-5;
    private static final int    SAMPLES_PER_PIECE = 64;
    private static final double WARM_UP_SECONDS = 3;
    private static final double MEASURED_SECONDS = 5;

    /** A circuit to run, and the two nodes whose signal says the answer is still right. */
    record Case(String circuit, String pulses, String filtered) {
    }

    /** How the whole circuit is traced for the sheet: not at all, as at real time, and every sample, as when slowed. */
    record Tracing(String name, int every) {
    }

    /** What the circuit answered: the pulses' frequency and duty, the filtered mean — so a faster wrong engine shows. */
    record Answer(double frequency, double duty, double mean) {
    }

    static final List<Case> CASES = List.of(
            new Case("pwm-filter-lab", "pwm", "out"),
            new Case("pwm-blocks-4", "pwm_1", "buffered_1"),
            new Case("bridge-rectifier", "a", "p"),
            new Case("square-rc", "in", "out"));

    static final List<Tracing> TRACINGS = List.of(
            new Tracing("untraced", 0),
            new Tracing("traced as at 1x", 250),
            new Tracing("traced every sample", 1));

    private LiveEngineBenchmark() {
    }

    public static void main(String[] arguments) {
        ComponentLibrary library = reference -> resource("components/" + reference + ".jmx");
        if (arguments.length == 2) {
            Case benchmark = CASES.stream().filter(candidate -> candidate.circuit().equals(arguments[0])).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No benchmark circuit " + arguments[0]));
            Run run = run(compile(benchmark, library), benchmark, Integer.parseInt(arguments[1]));
            Answer answer = answer(run);
            System.out.printf(Locale.ROOT, "RESULT %.1f %.2f %.5f %.5f%n", run.samplesPerSecond(), answer.frequency(),
                    answer.duty(), answer.mean());
            return;
        }
        System.out.printf("%-16s %-22s %14s %12s %10s %8s %10s%n",
                "circuit", "tracing", "samples/s", "x real time", "f, Hz", "duty", "mean, V");
        for (Case benchmark : CASES) {
            CompiledObject compiled = compile(benchmark, library);
            for (Tracing tracing : TRACINGS) {
                Run run = run(compiled, benchmark, tracing.every());
                Answer answer = answer(run);
                System.out.printf(Locale.ROOT, "%-16s %-22s %,14.0f %12.3f %10.1f %8.4f %10.4f%n", benchmark.circuit(),
                        tracing.name(), run.samplesPerSecond(), run.samplesPerSecond() * SAMPLE_INTERVAL,
                        answer.frequency(), answer.duty(), answer.mean());
            }
        }
    }

    private record Run(double samplesPerSecond, double[] pulses, double[] filtered) {
    }

    private static CompiledObject compile(Case benchmark, ComponentLibrary library) {
        DefinedObject defined = ExperienceDefinitions.read(requireResource("circuits/" + benchmark.circuit() + ".jmx"),
                benchmark.circuit());
        return DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(), library);
    }

    private static Run run(CompiledObject compiled, Case benchmark, int traceEvery) {
        LiveCircuitSession session = new LiveCircuitSession(new LiveCircuitSolver(compiled, SAMPLE_INTERVAL), List.of(
                new LiveCircuitSession.Probe("pulses", benchmark.pulses(), "0"),
                new LiveCircuitSession.Probe("filtered", benchmark.filtered(), "0")));
        session.trace(traceEvery);
        advanceFor(session, WARM_UP_SECONDS, null);
        List<LiveCircuitSession.Frame> kept = new ArrayList<>();
        long samples = advanceFor(session, MEASURED_SECONDS, kept);
        return new Run(samples / MEASURED_SECONDS, column(kept, "pulses"), column(kept, "filtered"));
    }

    /** Advances piece by piece for this many seconds of wall time; the number of samples it computed. */
    private static long advanceFor(LiveCircuitSession session, double seconds, List<LiveCircuitSession.Frame> kept) {
        long end = System.nanoTime() + (long) (seconds * 1e9);
        long samples = 0;
        while (System.nanoTime() < end) {
            LiveCircuitSession.Frame frame = session.advance(SAMPLES_PER_PIECE);
            samples += frame.time().length;
            if (kept != null) {
                kept.add(frame);
            }
        }
        return samples;
    }

    private static double[] column(List<LiveCircuitSession.Frame> frames, String channel) {
        return frames.stream().flatMapToDouble(frame -> Arrays.stream(frame.channels().get(channel))).toArray();
    }

    private static Answer answer(Run run) {
        double[] pulses = run.pulses();
        double low = Arrays.stream(pulses).min().orElse(0);
        double high = Arrays.stream(pulses).max().orElse(0);
        double level = (low + high) / 2;
        List<Integer> rising = new ArrayList<>();
        for (int index = 1; index < pulses.length; index++) {
            if (pulses[index - 1] < level && pulses[index] >= level) {
                rising.add(index);
            }
        }
        double frequency = Double.NaN;
        double duty = Double.NaN;
        if (rising.size() >= 2) {
            int first = rising.getFirst();
            int last = rising.getLast();
            frequency = (rising.size() - 1) / ((last - first) * SAMPLE_INTERVAL);
            long above = Arrays.stream(pulses, first, last).filter(value -> value >= level).count();
            duty = above / (double) (last - first);
        }
        return new Answer(frequency, duty, Arrays.stream(run.filtered()).average().orElse(Double.NaN));
    }

    private static Optional<String> resource(String path) {
        try (InputStream stream = LiveEngineBenchmark.class.getResourceAsStream("/circuit-benchmark/" + path)) {
            return stream == null ? Optional.empty() : Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static String requireResource(String path) {
        return resource(path).orElseThrow(() -> new IllegalStateException("No benchmark file " + path));
    }
}
