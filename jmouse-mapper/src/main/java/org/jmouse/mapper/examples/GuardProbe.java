package org.jmouse.mapper.examples;

import com.sun.management.ThreadMXBean;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.config.ReferenceMappingPolicy;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * The guards on the mapper's entry path, and what entering it actually costs. 🛡️
 *
 * <h2>⚠️ Why the guards need a driver of their own</h2>
 *
 * <p>Every performance ticket on this path trims something that is a <strong>guard</strong> — the depth
 * limit, the cycle tracking, the error policy — and a guard is invisible until the day it is the only
 * thing between a caller and a {@code StackOverflowError}. {@code FixProbe} checks that mapping
 * produces the right answers; nothing checked that the mapper still says <em>no</em> in the cases where
 * saying nothing at all would look identical until production.</p>
 *
 * <h2>⚠️ Why the cost is measured in BYTES and not in a profile percentage</h2>
 *
 * <p>A profile reports shares of a total, so a change that removes work makes everything else look
 * larger — two profiles taken either side of an improvement can show the improved frame growing. And a
 * warm microbenchmark hides allocation outright: escape analysis elides an object the JIT can prove
 * never leaves, so the loop measures nothing while a real workload pays every byte in GC.</p>
 *
 * <p>Allocated bytes per mapped object is neither. It is absolute, it is stable across runs on a laptop
 * that drifts 15–25% between sittings, and it is what the entry path actually spends.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class GuardProbe {

    private static final ThreadMXBean THREADS =
            (ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static int checked;
    private static int failures;

    private GuardProbe() {
    }

    public static void main(String... arguments) {
        verifyDepthGuardStillTrips();
        verifyCycleIsCarried();
        reportEntryCost();
        report();
    }

    /**
     * ⚠️ The check `JMF-167` recorded as missing from the repository.
     *
     * <p>A graph deeper than {@code maxDepth} is refused with {@link ErrorCodes#MAPPING_DEPTH_EXCEEDS}
     * rather than running until the stack ends. Built with a chain rather than a cycle, so it is the
     * <em>depth</em> limit being tested and not the cycle tracking that would otherwise catch it first.
     *
     * <h3>⚠️ The source and target types differ, and that is not decoration</h3>
     *
     * <p>Written first with one type on both sides, this check passed for the wrong reason: a property
     * whose declared type already matches the value is <strong>carried by reference</strong>, not
     * mapped, so {@code Node -> Node} nests exactly zero times however long the chain is. The depth
     * guard was never reached and the test proved nothing. Two types force a real step per link, which
     * is what the guard counts.</p>
     */
    private static void verifyDepthGuardStillTrips() {
        Mapper shallow = Mappers.builder()
                .config(MappingConfig.builder().maxDepth(4).build())
                .build();

        SourceNode deep = chain(12);

        try {
            shallow.map(deep, TargetNode.class);
            fail("a graph deeper than maxDepth is refused", "no exception was thrown");
        } catch (MappingException refused) {
            equal("a graph deeper than maxDepth is refused",
                  ErrorCodes.MAPPING_DEPTH_EXCEEDS, refused.code());
        }

        Mapper deepEnough = Mappers.builder()
                .config(MappingConfig.builder().maxDepth(64).build())
                .build();

        TargetNode mapped = deepEnough.map(deep, TargetNode.class);

        equal("and the same graph maps when the limit allows it", "0", mapped.getName());
        equal("all twelve links arrived", 12, length(mapped));
    }

    /**
     * A cycle is caught by {@code inProgress} rather than followed, and what happens then is the
     * caller's choice.
     *
     * <p>Two types again, for the reason above: with one, nothing is mapped and there is no cycle
     * tracking to exercise.</p>
     *
     * <h3>⚠️ All three policies are checked, because the default is the surprising one</h3>
     *
     * <p>{@link ReferenceMappingPolicy#BREAK} is the default and it <strong>cuts the link to
     * {@code null}</strong>. That is a deliberate choice and not a defect — written first as an
     * assertion that the loop comes back intact, this check failed, and the failure was the test's.
     * Asserting the default alongside {@link ReferenceMappingPolicy#PRESERVE}, which does bring the
     * loop back, is what stops the next reader making the same guess.</p>
     */
    private static void verifyCycleIsCarried() {
        SourceNode first = cycle();

        TargetNode broken = Mappers.defaultMapper().map(first, TargetNode.class);

        equal("a cycle does not run away", "first", broken.getName());
        equal("the second link is mapped, not carried", "second", broken.getNext().getName());
        equal("and BREAK, the default, cuts the loop", null, broken.getNext().getNext());

        Mapper preserving = Mappers.builder()
                .policy(MappingPolicy.builder()
                                .referenceMappingPolicy(ReferenceMappingPolicy.PRESERVE)
                                .build())
                .build();

        TargetNode preserved = preserving.map(cycle(), TargetNode.class);

        equal("PRESERVE brings the loop back",
              true, preserved == preserved.getNext().getNext());

        Mapper refusing = Mappers.builder()
                .policy(MappingPolicy.builder()
                                .referenceMappingPolicy(ReferenceMappingPolicy.FAIL)
                                .build())
                .build();

        try {
            refusing.map(cycle(), TargetNode.class);
            fail("FAIL refuses a cycle", "no exception was thrown");
        } catch (MappingException refused) {
            equal("FAIL refuses a cycle, and says why",
                  true, refused.getMessage() != null && !refused.getMessage().isBlank());
        }
    }

    /**
     * Two nodes pointing at each other.
     *
     * @return the first of them
     */
    private static SourceNode cycle() {
        SourceNode first  = new SourceNode();
        SourceNode second = new SourceNode();

        first.setName("first");
        second.setName("second");
        first.setNext(second);
        second.setNext(first);

        return first;
    }

    /**
     * How many distinct links a mapped chain has.
     *
     * @param head where to start
     * @return the number of nodes before the chain ends or loops
     */
    private static int length(TargetNode head) {
        int        links = 0;
        TargetNode node  = head;

        while (node != null && links < 100) {
            links++;
            node = node.getNext();
        }

        return links;
    }

    /**
     * What entering the mapper costs, in bytes, decomposed into what a property costs and what an
     * object costs before any property is looked at.
     */
    private static void reportEntryCost() {
        Mapper mapper = Mappers.defaultMapper();

        long one = allocatedPerObject(mapper, one(), One.class);
        long six = allocatedPerObject(mapper, six(), Six.class);

        long perProperty = (six - one) / 5;
        long perObject   = one - perProperty;

        System.out.printf("%n=== what entering the mapper costs ===%n");
        System.out.printf("  1 property   %5d bytes per mapped object%n", one);
        System.out.printf("  6 properties %5d bytes per mapped object%n", six);
        System.out.printf("  -> %d bytes per property, %d bytes fixed per object%n",
                          perProperty, perObject);
    }

    /**
     * Bytes allocated per mapped object, after the JIT has settled.
     *
     * @param mapper the mapper
     * @param source what to map
     * @param target what to map it into
     * @return bytes per object
     */
    private static long allocatedPerObject(Mapper mapper, Object source, Class<?> target) {
        int    rounds = 200_000;
        Object sink   = null;

        for (int warm = 0; warm < 100_000; warm++) {
            sink = mapper.map(source, target);
        }

        long before = THREADS.getCurrentThreadAllocatedBytes();

        for (int round = 0; round < rounds; round++) {
            sink = mapper.map(source, target);
        }

        long allocated = THREADS.getCurrentThreadAllocatedBytes() - before;

        // Read the sink so nothing above can be optimised away as dead.
        return sink == null ? -1 : allocated / rounds;
    }

    /**
     * A chain of nodes, so depth is reached without a cycle.
     *
     * @param length how many links
     * @return the head
     */
    private static SourceNode chain(int length) {
        List<SourceNode> nodes = new ArrayList<>(length);

        for (int step = 0; step < length; step++) {
            SourceNode node = new SourceNode();

            node.setName(String.valueOf(step));
            nodes.add(node);
        }

        for (int step = 0; step < length - 1; step++) {
            nodes.get(step).setNext(nodes.get(step + 1));
        }

        return nodes.getFirst();
    }

    private static One one() {
        One one = new One();

        one.setA("x");

        return one;
    }

    private static Six six() {
        Six six = new Six();

        six.setA("x");
        six.setB("y");
        six.setC("z");
        six.setD("1");
        six.setE("2");
        six.setF("3");

        return six;
    }

    private static void equal(String what, Object expected, Object actual) {
        checked++;

        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.printf("  ✗ %s: expected '%s', got '%s'%n", what, expected, actual);

            return;
        }

        System.out.printf("  ✓ %s%n", what);
    }

    private static void fail(String what, String why) {
        checked++;
        failures++;
        System.out.printf("  ✗ %s: %s%n", what, why);
    }

    private static void report() {
        if (failures == 0) {
            System.out.printf("%n%d guard checks, ALL PASS%n", checked);

            return;
        }

        System.out.printf("%n%d guard checks, %d failed%n", checked, failures);
        System.exit(1);
    }

    /**
     * A node that can point at another, for depth and for cycles.
     *
     * <p>⚠️ There are two of these, differing only in name, and the duplication is the test. A
     * property whose declared type already matches the value is carried by reference rather than
     * mapped, so one type on both sides nests zero times and exercises neither guard.</p>
     */
    public static class SourceNode {

        private String     name;
        private SourceNode next;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SourceNode getNext() {
            return next;
        }

        public void setNext(SourceNode next) {
            this.next = next;
        }
    }

    /** The other side of the pair — see {@link SourceNode}. */
    public static class TargetNode {

        private String     name;
        private TargetNode next;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TargetNode getNext() {
            return next;
        }

        public void setNext(TargetNode next) {
            this.next = next;
        }
    }

    /** One property, for the fixed half of the entry cost. */
    public static class One {

        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    /** Six properties, for the per-property half. */
    public static class Six {

        private String a;
        private String b;
        private String c;
        private String d;
        private String e;
        private String f;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }

        public String getD() {
            return d;
        }

        public void setD(String d) {
            this.d = d;
        }

        public String getE() {
            return e;
        }

        public void setE(String e) {
            this.e = e;
        }

        public String getF() {
            return f;
        }

        public void setF(String f) {
            this.f = f;
        }
    }
}
