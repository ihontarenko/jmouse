package org.jmouse.mapper.examples;

import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sampling profiler for the flat mapping path. 🔬
 *
 * <p>A daemon thread snapshots the worker's stack at a fixed interval and counts what it lands on.
 * Crude next to a real profiler, but it needs no tooling and it answers the only question that
 * matters when a change is proposed: which frame is actually holding the time.</p>
 *
 * <p>Three views are printed, and they answer three different questions:</p>
 *
 * <ul>
 *   <li><b>Self</b> - the top frame of each sample. Where the CPU actually was.</li>
 *   <li><b>Caller</b> - that same top frame paired with whoever called it. Where the CPU was is only
 *       half an answer; a shared helper burning time says nothing until you know which caller keeps
 *       reaching for it, and that is the frame you go and change.</li>
 *   <li><b>Total</b> - every frame present in the sample. How much time is spent below a method,
 *       inclusive, which is how a whole subsystem's share becomes visible.</li>
 * </ul>
 */
public class MappingProfile {

    private static final int  OBJECTS       = 20_000;
    private static final long DURATION_MS   = 12_000;
    private static final long SAMPLE_PERIOD = 1;
    private static final int  TOP           = 22;

    public static void main(String... arguments) throws Exception {
        Mapper                            mapper  = Mappers.defaultMapper();
        List<MappingBenchmark.FlatSource> sources = flatSources();

        Samples       samples = new Samples();
        AtomicBoolean running = new AtomicBoolean(true);

        Thread worker = new Thread(() -> {
            while (running.get()) {
                for (MappingBenchmark.FlatSource source : sources) {
                    mapper.map(source, MappingBenchmark.FlatTarget.class);
                }
            }
        }, "mapping-worker");

        Thread sampler = new Thread(() -> {
            while (running.get()) {
                samples.record(worker.getStackTrace());

                try {
                    Thread.sleep(SAMPLE_PERIOD);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "mapping-sampler");

        sampler.setDaemon(true);
        worker.start();
        sampler.start();

        Thread.sleep(DURATION_MS);
        running.set(false);
        worker.join();

        System.out.println("samples: " + samples.taken() + " over " + DURATION_MS + "ms");

        print("SELF - where the CPU actually was", samples.self(), samples.taken());
        print("CALLER - that frame, and who reached for it", samples.callers(), samples.taken());
        print("TOTAL - time spent below a frame, inclusive", samples.total(), samples.taken());
    }

    /**
     * The three counters a run accumulates, folded from one stack snapshot at a time.
     *
     * <p>Written by the sampler thread and read once the worker has stopped, so nothing here
     * synchronises: the read happens after the join.</p>
     */
    private static final class Samples {

        private final Map<String, Integer> self    = new HashMap<>();
        private final Map<String, Integer> total   = new HashMap<>();
        private final Map<String, Integer> callers = new HashMap<>();

        private int taken;

        /**
         * Fold one stack snapshot into the counters.
         *
         * <p>A snapshot can come back empty - the thread may be between states when it is asked -
         * and an empty one is not a sample of anything.</p>
         *
         * @param stack snapshot of the worker's stack, innermost frame first
         */
        void record(StackTraceElement[] stack) {
            if (stack.length == 0) {
                return;
            }

            taken++;

            String innermost = name(stack[0]);

            self.merge(innermost, 1, Integer::sum);

            if (stack.length > 1) {
                callers.merge(innermost + "  <-  " + name(stack[1]), 1, Integer::sum);
            }

            for (StackTraceElement frame : stack) {
                total.merge(name(frame), 1, Integer::sum);
            }
        }

        int taken() {
            return taken;
        }

        Map<String, Integer> self() {
            return self;
        }

        Map<String, Integer> total() {
            return total;
        }

        Map<String, Integer> callers() {
            return callers;
        }

        /**
         * @param frame one stack frame
         * @return the frame as {@code Class.method}, which is the granularity being counted
         */
        private static String name(StackTraceElement frame) {
            return frame.getClassName() + "." + frame.getMethodName();
        }
    }

    /**
     * Print the heaviest entries of one counter.
     *
     * @param heading section title
     * @param counter frame counts
     * @param samples total samples taken, for the percentage
     */
    private static void print(String heading, Map<String, Integer> counter, int samples) {
        System.out.println();
        System.out.println("=== " + heading + " ===");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counter.entrySet());

        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed());

        for (Map.Entry<String, Integer> entry : entries.subList(0, Math.min(TOP, entries.size()))) {
            double share = samples == 0 ? 0 : (100.0 * entry.getValue() / samples);
            System.out.printf("%6.2f%%  %s%n", share, shorten(entry.getKey()));
        }
    }

    /**
     * Trim package noise so the report fits a terminal.
     *
     * @param frame fully qualified frame name
     * @return shortened name
     */
    private static String shorten(String frame) {
        return frame
                .replace("org.jmouse.mapper.", "~mapping.")
                .replace("org.jmouse.core.access.", "~access.")
                .replace("org.jmouse.core.reflection.", "~reflection.")
                .replace("org.jmouse.core.convert.", "~convert.")
                .replace("org.jmouse.core.", "~core.")
                .replace("java.lang.", "")
                .replace("java.util.", "");
    }

    private static List<MappingBenchmark.FlatSource> flatSources() {
        List<MappingBenchmark.FlatSource> sources = new ArrayList<>(OBJECTS);

        for (int index = 0; index < OBJECTS; index++) {
            MappingBenchmark.FlatSource source = new MappingBenchmark.FlatSource();

            source.setId((long) index);
            source.setName("name-" + index);
            source.setEmail("user" + index + "@example.com");
            source.setAmount(new java.math.BigDecimal(index));
            source.setActive(index % 2 == 0);
            source.setCreatedAt(java.time.Instant.ofEpochMilli(index));

            sources.add(source);
        }

        return sources;
    }
}
