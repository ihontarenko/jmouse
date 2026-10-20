package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;

import org.jmouse.datamap.Quantity;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.SourceText;
import org.jmouse.parallax.component.ComponentModel.Expectation;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;
import org.jmouse.parallax.verification.ConsistencyCheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A component model held against its datasheet: every {@code expect} built into a circuit on its bench, simulated,
 * measured, and compared with what the datasheet says. A model that does not meet its own datasheet says so here,
 * point by point — the measured value, the window it had to fall in, and where in the datasheet that window is. It is
 * a check of the model, run with the catalogue's other consistency checks, not a way of showing it.
 *
 * <p>The bench is a netlist, one element per line:</p>
 * <pre>
 * V1 vcc 0 dc 15 V          — or step, sine 10 V 50 Hz, square 5 V 1 kHz
 * I1 a 0 dc 1 mA
 * R1 a b 10 kOhm · C1 a 0 10 nF · L1 a b 1 mH · D1 a k 1e-14
 * forward(current=10mA) or reverse(voltage=20V) - full two-terminal diode benches
 * U1 self vcc=vcc out=out A.inp=in      — the part under test, pins and parameter overrides by name
 * </pre>
 *
 * <p>What is measured on the bench, and how it is written, is {@link CircuitMeasurement}'s.</p>
 */
public final class ComponentCheck implements ConsistencyCheck {

    private static final String SELF = "self";

    public String code() {
        return "COMPONENT_DATASHEET";
    }

    public String description() {
        return "A component model meets every expectation its datasheet sets";
    }

    public boolean appliesTo(ObjectDefinition definition) {
        return definition instanceof ObjectDefinition.Component;
    }

    public CheckResult check(CompiledObject object) {
        String source = ((ObjectDefinition.Component) object.definition()).source();
        ComponentModel model = ComponentDocuments.read(source, "component", object.library());
        if (model.expectations().isEmpty()) {
            return CheckResult.skipped("The component sets no expectations.");
        }
        ComponentLibrary library = reference -> reference.equals(SELF) ? Optional.of(source) : object.library().source(reference);
        List<String> verdicts = new ArrayList<>();
        int failed = 0;
        for (Expectation expectation : model.expectations()) {
            String verdict;
            try {
                CircuitMeasurement.Measurement measurement = measure(expectation, model, library);
                boolean admitted = measurement.admittedBy(expectation.criterion());
                if (!admitted) {
                    failed++;
                }
                verdict = measurement.verdict(admitted, expectation.criterion());
            } catch (RuntimeException failure) {
                failed++;
                verdict = "ERROR " + failure.getMessage();
            }
            verdicts.add(expectation.title() + ": " + verdict + (expectation.reference() == null ? "" : " — " + expectation.reference()));
        }
        int total = model.expectations().size();
        // One verdict a line: glued with "; " they read as one paragraph, and a reference may hold a ";" of its own.
        String detail = (total - failed) + " of " + total + " expectations met.\n" + String.join("\n", verdicts);
        return new CheckResult(failed == 0 ? ConsistencyCheck.Outcome.PASSED : ConsistencyCheck.Outcome.FAILED, (double) failed,
                0.0, detail);
    }

    /** One expectation run: its bench built, simulated and measured. */
    private static CircuitMeasurement.Measurement measure(Expectation expectation, ComponentModel model, ComponentLibrary library) {
        Circuit circuit = bench(expectation, model);
        CompiledObject compiled = DefinitionCompiler.compile(circuit, List.of(), Map.of(), library);
        return CircuitMeasurement.of(expectation.measure()).takeOn(compiled, CircuitMeasurement.SAMPLES);
    }

    /** Standard two-terminal fixtures, each a whole bench: {@code X1 forward current = 10 mA}. */
    private static final Set<String> FIXTURES = Set.of("forward", "reverse");

    /**
     * The bench's parts as a circuit. A fixture is the whole bench; a part whose component is one of the solver's
     * primitives becomes that device; any other part — {@code self} included — is a model, its pins bound by name and
     * its settings overriding its parameters.
     */
    private static Circuit bench(Expectation expectation, ComponentModel model) {
        if (!(expectation.duration() > 0)) {
            throw new IllegalArgumentException("the bench says how long it runs: 'bench for 2 ms { … }'");
        }
        List<Block.Part> parts = benchParts(expectation, model);
        if (parts.stream().anyMatch(part -> FIXTURES.contains(part.reference()))) {
            if (parts.size() != 1) {
                throw new IllegalArgumentException("a standard bench fixture is the whole bench, not one part among others");
            }
            return atTemperature(new Circuit(fixture(parts.getFirst(), model), new Interval(0, expectation.duration())), expectation);
        }
        List<CircuitElement> elements = parts.stream().map(ComponentCheck::element).toList();
        return atTemperature(new Circuit(elements, new Interval(0, expectation.duration())), expectation);
    }

    /**
     * The parts an expectation runs on: the shared bench it names — the component's own, or a base's — and then its own,
     * none of them labelled twice.
     */
    private static List<Block.Part> benchParts(Expectation expectation, ComponentModel model) {
        if (expectation.sharedBench() == null) {
            return expectation.bench();
        }
        List<Block.Part> shared = model.benches().get(expectation.sharedBench());
        if (shared == null) {
            throw new IllegalArgumentException("there is no bench '" + expectation.sharedBench() + "' to stand on; "
                    + (model.benches().isEmpty() ? "it declares none: 'bench name { parts }'" : "its benches are " + model.benches().keySet()));
        }
        Set<String> labels = new HashSet<>();
        shared.forEach(part -> labels.add(part.id()));
        for (Block.Part part : expectation.bench()) {
            if (!labels.add(part.id())) {
                throw new IllegalArgumentException(part.id() + " is a part of bench '" + expectation.sharedBench()
                        + "' already; give this one another label");
            }
        }
        List<Block.Part> parts = new ArrayList<>(shared);
        parts.addAll(expectation.bench());
        return parts;
    }

    /**
     * The bench as its line sets it — {@code bench for 1 ms at 85 °C realistic} —: its temperature, and whether its
     * instruments load what they read; applied as a bench applies it.
     */
    private static Circuit atTemperature(Circuit circuit, Expectation expectation) {
        if (expectation.temperature() == null && !expectation.realistic()) {
            return circuit;
        }
        double temperature = expectation.temperature() != null ? expectation.temperature()
                : ObjectDefinition.BenchConditions.NOMINAL_TEMPERATURE;
        return new Circuit(circuit.elements(), circuit.time(), null, new ObjectDefinition.BenchConditions(
                temperature, 0, Map.of(), Map.of(), expectation.realistic(), Map.of()));
    }

    /**
     * {@code forward current = 10 mA}: a current source into the part's first pin; {@code reverse voltage = 20 V}: a
     * voltage across it, positive at the second. A two-terminal part declares its anode first.
     */
    private static List<CircuitElement> fixture(Block.Part part, ComponentModel model) {
        if (model.pins().size() != 2) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' tests a two-terminal part; '"
                    + model.name() + "' has " + model.pins().size() + " pins");
        }
        String anode = model.pins().get(0).name();
        String cathode = model.pins().get(1).name();
        boolean forward = part.reference().equals("forward");
        String required = forward ? "current" : "voltage";
        SourceText setting = part.settings().get(required);
        if (setting == null) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' needs " + required
                    + " = <quantity>, for example " + required + " = " + (forward ? "10 mA" : "20 V"));
        }
        double value = Quantity.valueOf(setting.text());
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("bench fixture '" + part.reference() + "' needs a positive finite " + required);
        }
        if (forward) {
            return List.of(new CircuitElement("I1", CircuitElementType.CURRENT_SOURCE,
                            "anode", "0", number(value), null, null, null),
                    new CircuitElement("U1", CircuitElementType.MODEL,
                            null, null, null, SELF, Map.of(anode, "anode", cathode, "0"), Map.of()));
        }
        return List.of(new CircuitElement("V1", CircuitElementType.VOLTAGE_SOURCE,
                        "cathode", "0", number(value), null, null, null),
                new CircuitElement("U1", CircuitElementType.MODEL,
                        null, null, null, SELF, Map.of(anode, "0", cathode, "cathode"), Map.of()));
    }

    /** A bench part line, read as a circuit's is: one reader of a part line, never a second. */
    private static CircuitElement element(Block.Part part) {
        return ExperienceDefinitions.element(part, "bench part '" + part.id() + "'");
    }

    private static String number(double value) {
        return Double.toString(value);
    }
}
