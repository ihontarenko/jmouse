package org.jmouse.mapper.examples;

import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.core.reflection.InferredType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Throughput driver for the mapping engine. 📈
 *
 * <p>Not a microbenchmark harness — no JMH here — but enough to answer "did that change help", which is
 * what it exists for.</p>
 *
 * <h2>⚠️ Every scenario is warmed before any is timed, and the rounds are interleaved</h2>
 *
 * <p>Running each scenario to completion in turn does not measure the scenarios; it measures their
 * <strong>order</strong>. The first one pays the engine's whole warm-up and every one after it inherits
 * it, and the ratio that produces is stable, repeatable and meaningless — stable enough to survive a
 * change that provably removed work from the profile.</p>
 *
 * <p>So the loop is inverted: one warm-up pass over <em>all</em> scenarios, then each timed round runs
 * every scenario once. Every scenario then sees the same JIT and the same GC weather, and the best round
 * per scenario is kept — the round least polluted by a pause landing mid-measurement.</p>
 *
 * <h2>⚠️ The headline is the ×control column, not the milliseconds</h2>
 *
 * <p>This machine drifts 15–25% between sittings, so two absolute figures from two runs are not a
 * comparison. The first scenario is therefore a <strong>hand-written copy of the same six properties</strong>
 * — the one thing in this file no change to the mapper can touch. Every other scenario is reported as a
 * multiple of it, and that multiple is what survives the machine having a different afternoon.</p>
 *
 * <p>For the two flat scenarios the multiple is also a like-for-like answer: <em>how many times slower
 * than writing the copy out by hand</em>. For the nested and collection scenarios the control is a
 * yardstick for the machine and nothing more.</p>
 *
 * <h2>⚠️ What this harness can and cannot resolve</h2>
 *
 * <p>Three consecutive runs with nothing changed put the flat scenario's ×control at 49.5, 48.6 and
 * 42.6. That spread is the instrument's resolution: <strong>a change worth under about 10% cannot be
 * told from a quiet afternoon here</strong>, and reporting one as an improvement would be reading the
 * weather. A change expected to be smaller than that needs the profile — {@code MappingProfile} — which
 * answers a different question and answers it precisely: not how fast, but where the time went.</p>
 *
 * <h2>⚠️ Nothing is allowed to be optimised away</h2>
 *
 * <p>A hand-written copy nobody reads is dead code, and a measurement of dead code measures whatever the
 * JIT left behind. Every scenario stores what it produced into {@link #sink}, which is {@code volatile}
 * and printed at the end — so the allocation escapes, the setters run, and the same small tax is paid by
 * the control and by the mapper alike.</p>
 */
public class MappingBenchmark {

    private static final int OBJECTS = 20_000;
    private static final int WARMUP  = 3;
    private static final int ROUNDS  = 5;

    /** The scenario every other one is reported against — see the note on the ×control column. */
    private static final String CONTROL = "hand-written copy — the control";

    /**
     * How many times the control repeats its pass within one round.
     *
     * <h2>⚠️ A ratio is only as steady as its denominator</h2>
     *
     * <p>A hand-written copy is roughly thirty times faster than a mapped one, so one pass over the same
     * list finishes in a fraction of a millisecond — short enough that timer granularity and a single
     * scheduling hiccup move it by a tenth. Divided into figures that were themselves steady to a few
     * per cent, that noise <strong>became</strong> the ×control column: the absolute milliseconds varied
     * by 3% across runs while the ratio built from them varied by 12%.</p>
     *
     * <p>So the control repeats until its round lasts as long as the rounds it normalises, and its object
     * count is multiplied to match. The yardstick has to be measured at least as carefully as the thing
     * being measured against it.</p>
     */
    private static final int CONTROL_REPEATS = 32;

    /**
     * Where every scenario puts what it produced.
     *
     * <p>⚠️ {@code volatile} on purpose. A plain field lets the JIT keep only the last store of a loop and
     * drop the rest of the work with it; a volatile store cannot be moved or removed, so what is timed is
     * what the code says.</p>
     */
    private static volatile Object sink;

    public static void main(String... arguments) {
        Mapper mapper = Mappers.defaultMapper();

        List<FlatSource>          flat   = flatSources(OBJECTS);
        List<NestedSource>        nested = nestedSources(OBJECTS / 4);
        List<Map<String, Object>> maps   = maps(OBJECTS);

        // ⚠️ Hoisted, and that is the entire content of this scenario. Allocated inside the loop it
        // allocated exactly as often as the scenario it is contrasted with, so the pair measured
        // something — but never "constructing versus not constructing", which is what its name claims.
        FlatTarget reused = new FlatTarget();

        // The default guard stops at 10k elements, and this list is deliberately larger.
        Mapper bulk = Mappers.builder()
                .config(MappingConfig.builder().maxCollectionSize(OBJECTS).build())
                .build();

        // Hoisted for the same reason a target is: building one per round times the builder, not the map.
        InferredType mapTree  = InferredType.forParametrizedClass(Map.class, String.class, Object.class);
        InferredType beanList = InferredType.forParametrizedClass(List.class, FlatTarget.class);

        List<Scenario> scenarios = List.of(
                new Scenario(CONTROL, OBJECTS * CONTROL_REPEATS,
                        () -> {
                            for (int repeat = 0; repeat < CONTROL_REPEATS; repeat++) {
                                for (FlatSource source : flat) {
                                    sink = copyByHand(source);
                                }
                            }
                        }),
                new Scenario("flat bean -> bean, 6 properties", OBJECTS,
                        () -> { for (FlatSource source : flat) { sink = mapper.map(source, FlatTarget.class); } }),
                new Scenario("flat bean -> existing instance", OBJECTS,
                        () -> { for (FlatSource source : flat) { sink = mapper.map(source, reused); } }),
                new Scenario("map -> bean, 6 properties", OBJECTS,
                        () -> { for (Map<String, Object> source : maps) { sink = mapper.map(source, FlatTarget.class); } }),
                new Scenario("nested bean -> bean, 3 levels + list", OBJECTS / 4,
                        () -> { for (NestedSource source : nested) { sink = mapper.map(source, NestedTarget.class); } }),
                new Scenario("bean -> Map<String,Object> tree", OBJECTS / 4,
                        () -> { for (NestedSource source : nested) { sink = mapper.map(source, mapTree); } }),
                new Scenario("List<bean> -> List<bean>, one call", OBJECTS,
                        () -> { sink = bulk.map(flat, beanList); }));

        report(measure(scenarios));
    }

    /**
     * Warms every scenario, then times them round by round, keeping each one's best round.
     *
     * @param scenarios what to measure, the control first
     * @return one result per scenario, in the order they were given
     */
    private static List<Result> measure(List<Scenario> scenarios) {
        // ⚠️ All of them, before any of them is timed. This is the whole fix: a scenario must not be able
        // to inherit — or pay — another one's warm-up.
        for (int round = 0; round < WARMUP; round++) {
            for (Scenario scenario : scenarios) {
                scenario.work().run();
            }
        }

        Map<String, Long> best = new LinkedHashMap<>();

        for (int round = 0; round < ROUNDS; round++) {
            for (Scenario scenario : scenarios) {
                long startedAt = System.nanoTime();
                scenario.work().run();

                best.merge(scenario.name(), System.nanoTime() - startedAt, Math::min);
            }
        }

        return scenarios.stream().map(scenario -> new Result(scenario, best.get(scenario.name()))).toList();
    }

    /**
     * Prints the table, and the reading of the machine the run came from.
     *
     * @param results one per scenario, the control among them
     */
    private static void report(List<Result> results) {
        Result control = results.stream()
                .filter(result -> CONTROL.equals(result.scenario().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("the control scenario is what everything "
                        + "else is reported against, and it is not in the results"));

        // ⚠️ Printed so that two runs can be told apart. A ratio is comparable across sittings; the
        // absolute figures under it are only comparable to figures taken on the same weather.
        System.out.printf("%s %s · %d processors · control %.1f ns/object%n",
                          System.getProperty("java.vm.name"),
                          System.getProperty("java.version"),
                          Runtime.getRuntime().availableProcessors(),
                          control.nanosecondsPerObject());
        System.out.println();

        System.out.printf("%-42s %10s %14s %10s%n", "scenario", "ms", "objects/s", "×control");
        System.out.println("-".repeat(80));

        for (Result result : results) {
            System.out.printf("%-42s %10.2f %14.0f %10.1f%n",
                              result.scenario().name(),
                              result.milliseconds(),
                              result.perSecond(),
                              result.nanosecondsPerObject() / control.nanosecondsPerObject());
        }

        System.out.printf("%n(sink: %s)%n", sink == null ? "null" : sink.getClass().getSimpleName());
    }

    /**
     * The same six properties, written out. The yardstick.
     *
     * <p>⚠️ Nothing in the mapping engine can change what this costs, which is the only property it is
     * required to have.</p>
     *
     * @param source what to copy
     * @return the copy
     */
    private static FlatTarget copyByHand(FlatSource source) {
        FlatTarget target = new FlatTarget();

        target.setId(source.getId());
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setAmount(source.getAmount());
        target.setActive(source.isActive());
        target.setCreatedAt(source.getCreatedAt());

        return target;
    }

    /**
     * One thing being measured.
     *
     * @param name    what it is, as the table prints it
     * @param objects how many objects one round maps, for the per-object and per-second figures
     * @param work    the round
     */
    private record Scenario(String name, int objects, Runnable work) {
    }

    /**
     * A scenario's best round.
     *
     * @param scenario    what was measured
     * @param nanoseconds how long its best round took
     */
    private record Result(Scenario scenario, long nanoseconds) {

        private double milliseconds() {
            return nanoseconds / 1_000_000.0;
        }

        private double perSecond() {
            return scenario.objects() / (nanoseconds / 1_000_000_000.0);
        }

        private double nanosecondsPerObject() {
            return (double) nanoseconds / scenario.objects();
        }
    }

    private static List<FlatSource> flatSources(int count) {
        List<FlatSource> sources = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            FlatSource source = new FlatSource();
            source.setId((long) index);
            source.setName("name-" + index);
            source.setEmail("user" + index + "@example.com");
            source.setAmount(new BigDecimal(index));
            source.setActive(index % 2 == 0);
            source.setCreatedAt(Instant.ofEpochMilli(index));
            sources.add(source);
        }

        return sources;
    }

    private static List<Map<String, Object>> maps(int count) {
        List<Map<String, Object>> sources = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("id", (long) index);
            source.put("name", "name-" + index);
            source.put("email", "user" + index + "@example.com");
            source.put("amount", new BigDecimal(index));
            source.put("active", index % 2 == 0);
            source.put("createdAt", Instant.ofEpochMilli(index));
            sources.add(source);
        }

        return sources;
    }

    private static List<NestedSource> nestedSources(int count) {
        List<NestedSource> sources = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            AddressSource address = new AddressSource();
            address.setCountry("Ukraine");
            address.setCity("Kyiv");
            address.setStreet("Khreshchatyk " + index);

            List<LineSource> lines = new ArrayList<>(4);
            for (int line = 0; line < 4; line++) {
                LineSource lineSource = new LineSource();
                lineSource.setSku("SKU-" + index + "-" + line);
                lineSource.setQuantity(line + 1);
                lineSource.setPrice(new BigDecimal(line + 1));
                lines.add(lineSource);
            }

            NestedSource source = new NestedSource();
            source.setId((long) index);
            source.setAddress(address);
            source.setLines(lines);
            sources.add(source);
        }

        return sources;
    }

    public static class FlatSource {
        private Long       id;
        private String     name;
        private String     email;
        private BigDecimal amount;
        private boolean    active;
        private Instant    createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public static class FlatTarget {
        private Long       id;
        private String     name;
        private String     email;
        private BigDecimal amount;
        private boolean    active;
        private Instant    createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public static class AddressSource {
        private String country;
        private String city;
        private String street;

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }

    public static class AddressTarget {
        private String country;
        private String city;
        private String street;

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }

    public static class LineSource {
        private String     sku;
        private int        quantity;
        private BigDecimal price;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class LineTarget {
        private String     sku;
        private int        quantity;
        private BigDecimal price;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class NestedSource {
        private Long             id;
        private AddressSource    address;
        private List<LineSource> lines;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public AddressSource getAddress() { return address; }
        public void setAddress(AddressSource address) { this.address = address; }
        public List<LineSource> getLines() { return lines; }
        public void setLines(List<LineSource> lines) { this.lines = lines; }
    }

    public static class NestedTarget {
        private Long             id;
        private AddressTarget    address;
        private List<LineTarget> lines;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public AddressTarget getAddress() { return address; }
        public void setAddress(AddressTarget address) { this.address = address; }
        public List<LineTarget> getLines() { return lines; }
        public void setLines(List<LineTarget> lines) { this.lines = lines; }
    }
}
