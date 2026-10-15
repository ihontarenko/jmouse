package org.jmouse.parallax.circuit.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Whether a change to the engine made it slower: the baseline engine and the candidate run every scenario of
 * {@link LiveEngineBenchmark} back to back, round after round, each scenario in a JVM of its own (one engine's JIT
 * profile never shapes the other's), the order swapped every round. What it reports is the **ratio** candidate /
 * baseline per scenario, the median over the rounds — this machine drifts 15–25 % within a session, so two runs at
 * different times say nothing, while two runs a few seconds apart drift together. It also refuses a candidate whose
 * answer moved: a faster wrong engine is not faster.
 *
 * <p>Each side is a classpath of its own — the engine's classes, these benchmark classes and their
 * {@code circuit-benchmark/} files, and the dependencies — so a candidate whose model language changed reads its own
 * copies of the circuits. Keep the baseline by copying {@code target/classes} and {@code target/test-classes} aside
 * before the change.</p>
 *
 * <pre>
 * java -cp &lt;candidate classpath&gt; org.jmouse.parallax.circuit.benchmark.PairedEngineBenchmark \
 *     "&lt;baseline classpath&gt;" "&lt;candidate classpath&gt;" [rounds] [circuit]
 * </pre>
 */
public final class PairedEngineBenchmark {

    /** What the harness resolves: three unchanged rounds spread about this much, so a smaller move is not a finding. */
    private static final double RESOLUTION = 0.08;

    private PairedEngineBenchmark() {
    }

    private record Result(double samplesPerSecond, double frequency, double duty, double mean) {
    }

    public static void main(String[] arguments) throws IOException, InterruptedException {
        if (arguments.length < 2) {
            throw new IllegalArgumentException("usage: PairedEngineBenchmark <baseline classpath> <candidate classpath> [rounds] [circuit]");
        }
        String baseline = arguments[0];
        String candidate = arguments[1];
        int rounds = arguments.length > 2 ? Integer.parseInt(arguments[2]) : 3;
        // One circuit only, when a change can touch nothing else: the rest are the same code on both sides.
        String only = arguments.length > 3 ? arguments[3] : null;
        System.out.printf("%-16s %-22s %14s %14s %8s  %s%n", "circuit", "tracing", "baseline/s", "candidate/s", "ratio", "answer");
        boolean slower = false;
        boolean moved = false;
        for (LiveEngineBenchmark.Case benchmark : LiveEngineBenchmark.CASES) {
            if (only != null && !only.equals(benchmark.circuit())) {
                continue;
            }
            for (LiveEngineBenchmark.Tracing tracing : List.of(LiveEngineBenchmark.TRACINGS.getFirst(), LiveEngineBenchmark.TRACINGS.getLast())) {
                List<Result> before = new ArrayList<>();
                List<Result> after = new ArrayList<>();
                for (int round = 0; round < rounds; round++) {
                    boolean baselineFirst = round % 2 == 0;
                    Result first = run(baselineFirst ? baseline : candidate, benchmark, tracing);
                    Result second = run(baselineFirst ? candidate : baseline, benchmark, tracing);
                    before.add(baselineFirst ? first : second);
                    after.add(baselineFirst ? second : first);
                }
                double[] ratios = new double[rounds];
                for (int round = 0; round < rounds; round++) {
                    ratios[round] = after.get(round).samplesPerSecond() / before.get(round).samplesPerSecond();
                }
                double ratio = median(ratios);
                boolean same = sameAnswer(before.getLast(), after.getLast());
                slower |= ratio < 1 - RESOLUTION;
                moved |= !same;
                System.out.printf(Locale.ROOT, "%-16s %-22s %,14.0f %,14.0f %8.3f  %s%n", benchmark.circuit(), tracing.name(),
                        median(before.stream().mapToDouble(Result::samplesPerSecond).toArray()),
                        median(after.stream().mapToDouble(Result::samplesPerSecond).toArray()), ratio,
                        same ? "same" : "MOVED: " + before.getLast() + " → " + after.getLast());
            }
        }
        System.out.printf(Locale.ROOT, "%nresolution ±%.0f %%: %s%n", RESOLUTION * 100,
                moved ? "the answer moved — the candidate is wrong, whatever its speed"
                        : slower ? "the candidate is SLOWER" : "no regression");
        if (moved || slower) {
            System.exit(1);
        }
    }

    private static Result run(String classpath, LiveEngineBenchmark.Case benchmark, LiveEngineBenchmark.Tracing tracing)
            throws IOException, InterruptedException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", classpath, LiveEngineBenchmark.class.getName(),
                benchmark.circuit(), String.valueOf(tracing.every())).redirectErrorStream(true).start();
        String result = null;
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                output.append(line).append('\n');
                if (line.startsWith("RESULT ")) {
                    result = line;
                }
            }
        }
        if (process.waitFor() != 0 || result == null) {
            throw new IllegalStateException("The benchmark did not run on " + classpath + ":\n" + output);
        }
        double[] values = Arrays.stream(result.substring("RESULT ".length()).trim().split(" ")).mapToDouble(Double::parseDouble).toArray();
        return new Result(values[0], values[1], values[2], values[3]);
    }

    /** The same answer: the frequency and the duty within a part in a thousand, the filtered mean within a millivolt. */
    private static boolean sameAnswer(Result before, Result after) {
        return Math.abs(after.frequency() - before.frequency()) <= before.frequency() * 1e-3
                && Math.abs(after.duty() - before.duty()) <= 1e-3
                && Math.abs(after.mean() - before.mean()) <= 1e-3;
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted.length % 2 == 1 ? sorted[sorted.length / 2] : (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2;
    }
}
