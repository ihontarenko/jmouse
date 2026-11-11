package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Given;
import org.jmouse.cad.Range;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Analysis;
import org.jmouse.cad.circuit.Bench;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Criterion;
import org.jmouse.cad.circuit.Expectation;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.EngineElement;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a circuit or a component says it does, held against what it does: every {@code expect} built into a circuit —
 * the circuit itself, or a component's bench —, simulated, measured as it says ({@link CircuitMeasurement}) and compared
 * with its window. A document that no longer does what it claims says so here, point by point: what was measured, the
 * window it had to fall in, and where that window comes from.
 */
public final class Expectations {

    /** The part a bench writes for the component under test. */
    private static final String SELF = "self";

    /** The standard two-terminal fixtures, each a whole bench: a current into the part, a voltage across it. */
    private static final String FORWARD = "forward";
    private static final String REVERSE = "reverse";

    /** The nets a fixture drives: the part's first pin, and its second. */
    private static final String ANODE = "anode";
    private static final String CATHODE = "cathode";

    /** How close a measured number is to an exact one to be it. */
    private static final double EXACTLY = 1e-9;

    private static final ExpressionLanguage ROWS = new ExpressionLanguage();

    private Expectations() {
    }

    /**
     * One expectation held: what was measured, the window it had to fall in, and whether it did; or why it could not be
     * measured at all.
     *
     * @param measured empty when the measurement was refused
     * @param refusal  why it could not be measured; empty when it was
     */
    public record Verdict(String title, String source, Optional<Measured> measured, String window, boolean met, String refusal) {

        /** {@code PASS 4.9873 — must be [4.9, 5.1]}, {@code FAIL …} or {@code ERROR …}, as a check reports it. */
        public String written() {
            if (measured.isEmpty()) {
                return "ERROR " + refusal;
            }
            return (met ? "PASS " : "FAIL ") + measured.get().written() + " — must be " + window;
        }
    }

    /** Every expectation a circuit states of itself, run on the circuit as written, on the nominal bench. */
    public static List<Verdict> ofCircuit(Circuit circuit, CatalogueDocuments catalogue) {
        Map<String, Double> values = new LinkedHashMap<>();
        ParameterEvaluation.evaluate(circuit.head().title(), circuit.parameters()).forEach(parameter ->
                values.put(parameter.name(), parameter.value()));
        MeasuredCircuit measured = new MeasuredCircuit(circuit, catalogue, BenchConditions.nominal(), List.of());
        List<Verdict> verdicts = new ArrayList<>();
        for (Expectation expectation : circuit.expectations()) {
            verdicts.add(hold(expectation, () -> measured, values));
        }
        return verdicts;
    }

    /**
     * Every expectation a component's datasheet sets, each on its bench: the shared bench it names — the component's own
     * or a base's —, then its own parts, the component under test written {@code self}.
     */
    public static List<Verdict> ofComponent(DocumentNode document, CatalogueDocuments catalogue) {
        Component component = SimulationReading.component(document);
        CatalogueDocuments withSelf = slug -> slug.equals(SELF) ? Optional.of(document) : catalogue.find(slug);
        SimulatedComponent compiled = ComponentCompilation.compile(component, catalogue);
        Map<String, Double> values = new LinkedHashMap<>();
        for (EvaluatedParameter parameter : compiled.parameters()) {
            values.put(parameter.name(), parameter.value());
        }
        List<Verdict> verdicts = new ArrayList<>();
        for (Expectation expectation : component.expectations()) {
            verdicts.add(hold(expectation, () -> bench(expectation, compiled, withSelf), values));
        }
        return verdicts;
    }

    /** What builds the circuit an expectation is measured on — refused, it is the expectation's refusal. */
    @FunctionalInterface
    private interface Built {
        MeasuredCircuit circuit();
    }

    private static Verdict hold(Expectation expectation, Built built, Map<String, Double> values) {
        String window;
        try {
            window = window(expectation.criterion(), values);
        } catch (RuntimeException unreadable) {
            return new Verdict(expectation.title(), expectation.source(), Optional.empty(), "", false, unreadable.getMessage());
        }
        try {
            Measured measured = CircuitMeasurement.take(expectation.measure(), built.circuit());
            return new Verdict(expectation.title(), expectation.source(), Optional.of(measured), window,
                    admits(expectation.criterion(), measured, values), "");
        } catch (RuntimeException failure) {
            return new Verdict(expectation.title(), expectation.source(), Optional.empty(), window, false,
                    String.valueOf(failure.getMessage()));
        }
    }

    // ─── the bench ────────────────────────────────────────────────────────────────────────────────────

    private static MeasuredCircuit bench(Expectation expectation, SimulatedComponent component, CatalogueDocuments catalogue) {
        Bench bench = expectation.ownBench().orElseThrow(() -> new IllegalArgumentException(
                "an expectation of a component runs on a bench: 'bench(for: 2 ms) { … }'"));
        if (bench.duration() == null || !(bench.duration() > 0)) {
            throw new IllegalArgumentException("the bench says how long it runs: 'bench(for: 2 ms) { … }'");
        }
        List<Part> parts = benchParts(bench, component);
        List<EngineElement> fixtures = new ArrayList<>();
        if (parts.stream().anyMatch(part -> part.component().equals(FORWARD) || part.component().equals(REVERSE))) {
            if (parts.size() != 1) {
                throw new IllegalArgumentException("a standard bench fixture is the whole bench, not one part among others");
            }
            Part fixture = parts.getFirst();
            parts = List.of(fixture(fixture, component, fixtures));
        }
        Circuit circuit = new Circuit(new DocumentHead(component.slug() + "-bench", component.title() + " on its bench", null, Map.of()),
                List.of(), parts, List.of(), List.of(), null, List.of(), new Analysis(SimulationVocabulary.TRANSIENT, bench.duration()), List.of(), null,
                SheetBoundary.NONE);
        return new MeasuredCircuit(circuit, catalogue, BenchConditions.of(bench), fixtures);
    }

    /** The shared bench an expectation names — its parts first —, then its own; none of them labelled twice. */
    private static List<Part> benchParts(Bench bench, SimulatedComponent component) {
        if (bench.shared().isEmpty()) {
            return bench.parts();
        }
        String name = bench.shared().get();
        Bench shared = null;
        for (Component written : component.lineage()) {
            if (written.bench() != null && name.equals(written.bench().name())) {
                shared = written.bench();
            }
        }
        if (shared == null) {
            throw new IllegalArgumentException("there is no bench '" + name + "' to stand on: 'bench(" + name + ") { parts }'");
        }
        Set<String> references = new HashSet<>();
        shared.parts().forEach(part -> references.add(part.reference()));
        for (Part part : bench.parts()) {
            if (!references.add(part.reference())) {
                throw new IllegalArgumentException(part.reference() + " is a part of bench '" + name + "' already; give this one "
                        + "another reference");
            }
        }
        List<Part> parts = new ArrayList<>(shared.parts());
        parts.addAll(bench.parts());
        return parts;
    }

    /**
     * {@code part:X1(forward, current: 10 mA)}: a current source into the part's first pin; {@code part:X1(reverse,
     * voltage: 20 V)}: a voltage across it, positive at the second. A two-terminal part declares its anode first. The
     * sources are the bench's own; the part under test is wired between them and ground.
     */
    private static Part fixture(Part fixture, SimulatedComponent component, List<EngineElement> into) {
        List<Pin> pins = component.pins();
        if (pins.size() != 2) {
            throw new IllegalArgumentException("bench fixture '" + fixture.component() + "' tests a two-terminal part; '"
                    + component.title() + "' has " + pins.size() + " pins");
        }
        boolean forward = fixture.component().equals(FORWARD);
        String required = forward ? "current" : "voltage";
        Given written = fixture.setting(required).flatMap(Setting::given).orElseThrow(() -> new IllegalArgumentException(
                "bench fixture '" + fixture.component() + "' needs " + required + ": <quantity>, for example " + required + ": "
                        + (forward ? "10 mA" : "20 V")));
        if (!(written instanceof Formula quantity)) {
            throw new IllegalArgumentException("bench fixture '" + fixture.component() + "' needs " + required + " as a quantity");
        }
        double value = Quantity.valueOf(quantity.written());
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("bench fixture '" + fixture.component() + "' needs a positive finite " + required);
        }
        String anode = pins.get(0).name();
        String cathode = pins.get(1).name();
        if (forward) {
            into.add(new EngineElement("I1", EngineElement.Kind.CURRENT_SOURCE, ANODE, Circuit.GROUND, value));
            return new Part("U1", SELF, List.of(new Connection(anode, ANODE), new Connection(cathode, Circuit.GROUND)), List.of(),
                    null, null);
        }
        into.add(new EngineElement("V1", EngineElement.Kind.VOLTAGE_SOURCE, CATHODE, Circuit.GROUND, value));
        return new Part("U1", SELF, List.of(new Connection(anode, Circuit.GROUND), new Connection(cathode, CATHODE)), List.of(),
                null, null);
    }

    // ─── the window ───────────────────────────────────────────────────────────────────────────────────

    /** The window as a verdict shows it: {@code [4.9, 5.1]}, or the exact value. */
    private static String window(Criterion criterion, Map<String, Double> values) {
        return switch (criterion) {
            case Criterion.Is is -> is.value().written();
            default -> {
                double[] bounds = bounds(criterion, values);
                yield "[" + format(bounds[0]) + ", " + format(bounds[1]) + "]";
            }
        };
    }

    private static boolean admits(Criterion criterion, Measured measured, Map<String, Double> values) {
        if (criterion instanceof Criterion.Is is) {
            return same(ROWS.evaluate(is.value().written()), measured);
        }
        if (!(measured instanceof Measured.Value(double value))) {
            return false;
        }
        double[] bounds = bounds(criterion, values);
        return value >= bounds[0] && value <= bounds[1];
    }

    /**
     * Where a window runs: {@code near(5 V, +- 2%)} a share of its typical value either side of it — or an amount, when
     * written as one —, {@code within(4.9 V .. 5.1 V)} its two ends. A value is a quantity or an expression of the
     * parameters it is read over.
     */
    private static double[] bounds(Criterion criterion, Map<String, Double> values) {
        return switch (criterion) {
            case Criterion.Near near -> {
                double centre = value(near.value(), values);
                double spread = Math.abs(value(near.tolerance(), values));
                boolean relative = Quantity.parse(near.tolerance().written()).map(Quantity::unit).filter("%"::equals).isPresent();
                double width = relative ? Math.abs(centre) * spread : spread;
                yield new double[] {centre - width, centre + width};
            }
            case Criterion.Within within when within.range() instanceof Range.Between between ->
                    new double[] {value(between.low(), values), value(between.high(), values)};
            case Criterion.Within within -> throw new IllegalArgumentException("'within' is a span, 'within(4.9 V .. 5.1 V)'");
            case Criterion.Is is -> throw new IllegalStateException("an 'is' criterion has no interval");
        };
    }

    private static double value(Formula written, Map<String, Double> values) {
        return ParameterEvaluation.value("a window", written, values);
    }

    /** Whether what was measured is the exact value written: the same number, or the same row item by item. */
    private static boolean same(Object exact, Measured measured) {
        return switch (measured) {
            case Measured.Value(double value) -> exact instanceof Number number && Math.abs(number.doubleValue() - value) < EXACTLY;
            case Measured.Row(List<Object> items) -> {
                // jME reads a row written in brackets as an array.
                List<?> row = exact instanceof Object[] array ? List.of(array) : exact instanceof List<?> list ? list : null;
                if (row == null || row.size() != items.size()) {
                    yield false;
                }
                for (int index = 0; index < items.size(); index++) {
                    if (!sameItem(row.get(index), items.get(index))) {
                        yield false;
                    }
                }
                yield true;
            }
        };
    }

    private static boolean sameItem(Object exact, Object measured) {
        if (exact instanceof Number expected && measured instanceof Number found) {
            return Math.abs(expected.doubleValue() - found.doubleValue()) < EXACTLY;
        }
        return String.valueOf(exact).equals(String.valueOf(measured));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.5g", value);
    }
}
