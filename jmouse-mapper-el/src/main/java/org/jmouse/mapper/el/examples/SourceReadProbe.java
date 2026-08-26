package org.jmouse.mapper.el.examples;

import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.JmmRuleSource;

/**
 * How many times a mapping reads the object it is mapping. 🔢
 *
 * <h2>⚠️ A counter, not a stopwatch, and deliberately</h2>
 *
 * <p>The thing being checked here is that a computed rule reads the properties it <em>names</em> and no
 * others. A benchmark cannot resolve that — it is a few per cent of wall clock on this shape, well under
 * what {@code MappingBenchmark} can tell from a quiet afternoon — and a profile only says which frame
 * the time went to. A count of reads is exact, is the same on every machine, and fails loudly the day
 * somebody reinstates the sweep.</p>
 *
 * <h2>What it used to be</h2>
 *
 * <p>Every computed rule was evaluated against a context filled by reading <strong>every readable
 * property of the source</strong> first — per rule, not per object, because each rule hands the engine
 * its own lambda and each lambda built its own context. Twelve properties and four computed rules is
 * <strong>48 reads</strong> to serve the six the expressions mention.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceReadProbe {

    /**
     * ⚠️ Four computed rules over a twelve-property source, and the target has nothing else — so every
     * read of the source is one an expression asked for, and none is a same-named copy.
     */
    private static final String FILE = """
            mapping "reads" {
                use org.jmouse.mapper.el.examples.SourceReadProbe$Wide
                use org.jmouse.mapper.el.examples.SourceReadProbe$Narrow

                target Narrow {
                    from Wide {
                        one   : alpha ~ "-" ~ bravo
                        two   : charlie | upper
                        three : delta ~ echo
                        four  : foxtrot | default("none")
                    }
                }
            }
            """;

    /** The six roots the four rules above mention, counted once each. */
    private static final int NAMED = 6;

    private static int failures = 0;

    private SourceReadProbe() {
    }

    public static void main(String... arguments) {
        JmmRuleSource rules  = new JmmReader().read(FILE, "reads.jmm");
        Mapper        mapper = Mappers.builder().rules(builder -> builder.ruleSource(rules)).build();

        Wide source = new Wide();

        // ⚠️ Mapped once cold to let anything cached settle, then measured on a clean count. A plan is
        // compiled on first use, and compiling it is allowed to look at whatever it likes.
        mapper.map(source, Narrow.class);

        Wide.reads = 0;

        Narrow mapped = mapper.map(source, Narrow.class);

        report("the mapping is right", mapped.getOne() + "|" + mapped.getTwo(), "a-b|C");
        report("...and the rest of it", mapped.getThree() + "|" + mapped.getFour(), "de|f");

        System.out.printf("%n  source has 12 readable properties, 4 computed rules name %d of them%n",
                          NAMED);
        System.out.printf("  reads per mapped object: %d   (it was 4 x 12 = 48)%n%n", Wide.reads);

        report("a computed rule reads only what it names", Wide.reads, NAMED);

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILED");
    }

    private static void report(String what, Object actual, Object expected) {
        boolean passed = expected.equals(actual);

        if (!passed) {
            failures++;
        }

        System.out.printf("%s  %-44s -> %s%s%n", passed ? "PASS" : "FAIL", what, actual,
                          passed ? "" : "  (expected " + expected + ")");
    }

    /** Twelve readable properties, each of which says when it was read. */
    public static class Wide {

        /** ⚠️ Static, because the counter has to survive the mapper building its own instances. */
        public static int reads = 0;

        public String getAlpha()   { reads++; return "a"; }
        public String getBravo()   { reads++; return "b"; }
        public String getCharlie() { reads++; return "c"; }
        public String getDelta()   { reads++; return "d"; }
        public String getEcho()    { reads++; return "e"; }
        public String getFoxtrot() { reads++; return "f"; }
        public String getGolf()    { reads++; return "g"; }
        public String getHotel()   { reads++; return "h"; }
        public String getIndia()   { reads++; return "i"; }
        public String getJuliett() { reads++; return "j"; }
        public String getKilo()    { reads++; return "k"; }
        public String getLima()    { reads++; return "l"; }
    }

    /** Four properties, none of them named the same as anything on the source. */
    public static class Narrow {

        private String one;
        private String two;
        private String three;
        private String four;

        public String getOne() { return one; }
        public void setOne(String one) { this.one = one; }
        public String getTwo() { return two; }
        public void setTwo(String two) { this.two = two; }
        public String getThree() { return three; }
        public void setThree(String three) { this.three = three; }
        public String getFour() { return four; }
        public void setFour(String four) { this.four = four; }
    }
}
