package org.jmouse.parallax.spice;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.circuit.CircuitComputations;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;
import org.jmouse.spice.SpiceParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The converter against the catalogue's own components (copies under {@code spice/components/}): what it writes is read
 * back, compiled and — where the circuit says something measurable — run, and its answer checked against the circuit's
 * own arithmetic.
 */
class SpiceConverterTest {

    private static final Map<String, String> EXTRA = new HashMap<>();

    /**
     * The library's own catalogue, by slug — never a copy of it, which drifts from the documents the engine ships —,
     * and under {@code spice/components/} only the datasheet parts a product ships rather than the library (1N4148,
     * 2N2222, 2N7000).
     */
    private static final Map<String, String> CATALOGUE = catalogue();

    private static final ComponentLibrary LIBRARY = reference -> EXTRA.containsKey(reference)
            ? Optional.of(EXTRA.get(reference))
            : resource("spice/components/" + reference + ".jmx").or(() -> Optional.ofNullable(CATALOGUE.get(reference)));

    private final SpiceConverter converter = new SpiceConverter(LIBRARY);

    // ─── SPICE → jMX ────────────────────────────────────────────────────────────────────────────────

    @Test
    void aResistorDividerIsACircuitThatDividesAsItsResistancesSay() {
        SpiceConverter.SpiceImport imported = converter.importNetlist("""
                Resistor divider
                V1 in 0 DC 9
                R1 in out 1k
                R2 out 0 2k
                .tran 1u 1m
                .end
                """, null);
        assertEquals(Kind.CIRCUIT, imported.kind());
        assertTrue(imported.source().contains("R1 resistor (in, out) resistance = 1 kOhm"), imported.source());
        assertTrue(imported.source().contains("V1 voltage-source (in, 0) U_output = 9 V"), imported.source());
        assertTrue(imported.source().contains("analysis transient for 1 ms"), imported.source());
        assertTrue(imported.notes().isEmpty(), imported.notes().toString());
        assertEquals(6, last(simulate(imported.source()), "out"), 1e-6);
    }

    @Test
    void anRcLowPassWithASineAndParametersPassesItsCornerAtSeventyPercent() {
        SpiceConverter.SpiceImport imported = converter.importNetlist("""
                RC low-pass at its corner
                .param R=1k C=1u
                V1 in 0 SIN(0 1 159.1549)
                R1 in out {R}
                C1 out 0 {C}
                .tran 10u 50m
                .end
                """, "Corner of an RC");
        String source = imported.source();
        assertTrue(source.startsWith("circuit \"Corner of an RC\" as corner-of-an-rc {"), source);
        assertTrue(source.contains("R = 1 kOhm"), source);
        assertTrue(source.contains("C = 1 uF"), source);
        assertTrue(source.contains("R1 resistor (in, out) resistance = R"), source);
        assertTrue(source.contains("V1 sine-generator (positive = in, negative = 0) amplitude = 1 V, frequency = 159.1549 Hz"),
                source);
        Dataset.TimeSeries series = simulate(source);
        double peak = maximumAfter(series, "out", 0.03);
        assertEquals(1 / Math.sqrt(2), peak, 0.02, "the output at the corner is 1/√2 of the input");
    }

    @Test
    void aSubcircuitWithAModelIsAComponentThatWorksOnABench() {
        SpiceConverter.SpiceImport imported = converter.importNetlist("""
                * a clamp: a resistor in series and a junction to its return
                .subckt clamp in out ret params: R=1k
                R1 in out {R}
                C1 out ret 100p
                D1 out ret DCLAMP
                .model DCLAMP D(IS=2.52n RS=0.568 N=1.752 CJO=4p M=0.4 TT=20n BV=100 IBV=100u)
                .ends clamp
                """, null);
        assertEquals(Kind.COMPONENT, imported.kind());
        String source = imported.source();
        assertTrue(source.contains("in analog 1"), source);
        assertTrue(source.contains("R = 1 kOhm"), source);
        assertTrue(source.contains("D1 diode (anode = out, cathode = ret, "), source);
        assertTrue(imported.notes().stream().anyMatch(note -> note.contains("DCLAMP names no component")), imported.notes().toString());

        EXTRA.put("clamp", source);
        try {
            String bench = """
                    circuit "Clamp bench" as clamp-bench {
                        parts {
                            V1 sine-generator (positive = in, negative = 0) amplitude = 5 V, frequency = 50 Hz
                            U1 clamp (in = in, out = out, ret = 0)
                        }
                        analysis transient for 40 ms
                    }
                    """;
            Dataset.TimeSeries series = simulate(bench);
            double clamped = maximumAfter(series, "out", 0.02);
            assertTrue(clamped > 0.55 && clamped < 0.9, "the junction holds the output near 0.7 V, not " + clamped);
        } finally {
            EXTRA.remove("clamp");
        }
    }

    @Test
    void aBridgeOfFourDiodesRectifiesLessTwoDrops() {
        SpiceConverter.SpiceImport imported = converter.importNetlist("""
                Bridge rectifier
                VAC a b SIN(0 10 50)
                D1 a p D1N4148
                D2 b p D1N4148
                D3 0 a D1N4148
                D4 0 b D1N4148
                RL p 0 1k
                .model D1N4148 D(IS=2.52n RS=0.568 N=1.752 CJO=4p M=0.4 TT=20n BV=100 IBV=100u)
                .tran 10u 40m
                .end
                """, null);
        String source = imported.source();
        assertTrue(source.contains("D3 diode (anode = 0, cathode = a)"), source);
        double peak = maximumAfter(simulate(source), "p", 0.02);
        assertTrue(peak > 8.0 && peak < 9.0, "10 V less two forward drops, not " + peak);
    }

    @Test
    void namesWhatItLeavesOutAndWhy() {
        SpiceConverter.SpiceImport imported = converter.importNetlist("""
                Things jMX has not
                .include "models.lib"
                V1 in 0 DC 1 AC 1
                E1 out 0 in 0 10
                R1 out 0 1M
                .ac dec 10 1 1meg
                .tran 1u 1m uic
                .end
                """, null);
        List<String> notes = imported.notes();
        assertTrue(notes.stream().anyMatch(note -> note.startsWith("line 2:") && note.contains("no file of that name was given")), notes.toString());
        // The catalogue has a voltage-controlled voltage source, so E1 comes across as one rather than being left out.
        assertTrue(imported.source().contains("E1 voltage-controlled-voltage-source"), imported.source());
        assertTrue(notes.stream().anyMatch(note -> note.startsWith("line 3:") && note.contains("AC")), notes.toString());
        assertTrue(notes.stream().anyMatch(note -> note.contains("'1M' is read as SPICE reads it, a thousandth")), notes.toString());
        assertTrue(notes.stream().anyMatch(note -> note.startsWith("line 6:") && note.contains("AC sweep")), notes.toString());
        assertTrue(notes.stream().anyMatch(note -> note.contains("uic")), notes.toString());
    }

    @Test
    void refusesWhatIsNotSpiceWithItsLine() {
        SpiceParseException noValue = assertThrows(SpiceParseException.class,
                () -> converter.importNetlist("broken\nR1 a b\n", null));
        assertEquals(2, noValue.getPosition().line());
        SpiceParseException unknownName = assertThrows(SpiceParseException.class,
                () -> converter.importNetlist("broken\nR1 a 0 {2*Rx}\n.tran 1u 1m\n", null));
        assertEquals(2, unknownName.getPosition().line());
        assertTrue(unknownName.getDetail().contains("'Rx' is not a parameter"), unknownName.getDetail());
        assertThrows(SpiceParseException.class, () -> converter.importNetlist("only a title\n", null));
        assertThrows(ExperienceParseException.class, () -> converter.exportDocument("circuit \"x\" { parts {"));
        assertThrows(SpiceParseException.class, () -> converter.exportDocument(
                "surface \"Wave\" as wave {\n    inputs {\n        x in -1 .. 1\n        y in -1 .. 1\n    }\n"
                        + "    outputs {\n        z : surface = x * y\n    }\n}\n"));
    }

    // ─── jMX → SPICE ────────────────────────────────────────────────────────────────────────────────

    @Test
    void writesComponentsAsTheModelsAndSubcircuitsSpiceHas() {
        String diode = converter.exportDocument(resource("spice/components/1n4148.jmx").orElseThrow()).text();
        assertTrue(diode.contains(".model 1n4148 D(IS=2.52e-9 N=1.752 RS=0.568"), diode);
        assertTrue(diode.contains("TNOM=27"), diode);

        String potentiometer = converter.exportDocument(LIBRARY.source("potentiometer").orElseThrow()).text();
        assertTrue(potentiometer.contains(".subckt potentiometer a w b params: position=0.5 resistance=10000"), potentiometer);
        assertTrue(potentiometer.contains("B1 a w I="), potentiometer);
        assertTrue(potentiometer.contains("V(a)"), potentiometer);

        SpiceConverter.SpiceExport ammeter = converter.exportDocument(LIBRARY.source("ammeter").orElseThrow());
        assertTrue(ammeter.text().contains("B1 in out V=0"), ammeter.text());
        assertTrue(ammeter.notes().stream().anyMatch(note -> note.contains("ideal bench")), ammeter.notes().toString());

        SpiceParseException generator = assertThrows(SpiceParseException.class, () -> converter.exportDocument(
                resource("spice/components/2n7000.jmx").orElseThrow().replace("type mosfet", "type amplifier")));
        assertTrue(generator.getDetail().contains("cannot be a SPICE subcircuit"), generator.getDetail());
    }

    @Test
    void aCatalogueCircuitComesBackFromSpiceAsItWent() {
        for (String circuit : List.of("rc-low-pass", "full-wave-rectifier", "bjt-switch")) {
            String original = resource("spice/circuits/" + circuit + ".jmx").orElseThrow();
            SpiceConverter.SpiceExport exported = converter.exportDocument(original);
            assertTrue(exported.notes().stream().anyMatch(note -> note.contains("layout")) || !original.contains("layout"),
                    exported.notes().toString());
            SpiceConverter.SpiceImport imported = converter.importNetlist(exported.text(), null);
            assertEquals(Kind.CIRCUIT, imported.kind());
            assertSameCircuit(original, imported.source(), circuit + "\n" + exported.text() + "\n" + imported.source());
        }
    }

    /** The same parts on the same nodes with the same values, and the same span; ids may have gained SPICE's letter. */
    private static void assertSameCircuit(String original, String roundTripped, String context) {
        CompiledObject before = compile(original);
        CompiledObject after = compile(roundTripped);
        ObjectDefinition.Circuit first = (ObjectDefinition.Circuit) before.definition();
        ObjectDefinition.Circuit second = (ObjectDefinition.Circuit) after.definition();
        assertEquals(first.time(), second.time(), context);
        for (CircuitElement element : first.elements()) {
            CircuitElement match = second.elements().stream()
                    .filter(candidate -> candidate.id().equals(element.id()) || candidate.id().substring(1).equals(element.id()))
                    .findFirst().orElse(null);
            if (element.type() == ObjectDefinition.CircuitElementType.MODEL && isInstrument(element.component())) {
                continue;
            }
            assertNotNull(match, element.id() + " is missing\n" + context);
            assertEquals(componentOf(element), componentOf(match), element.id() + "\n" + context);
            assertEquals(terminals(element), terminals(match), element.id() + "\n" + context);
            String value = valueOf(element);
            if (value != null) {
                assertEquals(before.constant(value), after.constant(valueOf(match)),
                        1e-9 * Math.abs(before.constant(value)), element.id() + "\n" + context);
            }
            for (Map.Entry<String, String> override : element.overrides().entrySet()) {
                if (override.getKey().equals("power_rating") || override.getKey().equals("thermal_resistance")
                        || override.getKey().equals(VALUE_SETTINGS.get(componentOf(element)))) {
                    continue;
                }
                assertTrue(match.overrides().containsKey(override.getKey()), override + "\n" + context);
                assertEquals(before.constant(override.getValue()), after.constant(match.overrides().get(override.getKey())),
                        1e-9 * Math.abs(before.constant(override.getValue())), override + "\n" + context);
            }
        }
    }

    /** Each primitive's pins in order, as its component names them, and the setting its value is written in. */
    private static final Map<String, List<String>> PRIMITIVE_PINS = Map.of(
            "resistor", List.of("terminal_one", "terminal_two"), "capacitor", List.of("terminal_one", "terminal_two"),
            "inductor", List.of("terminal_one", "terminal_two"), "diode", List.of("anode", "cathode"),
            "voltage-source", List.of("positive", "negative"), "current-source", List.of("positive", "negative"));
    private static final Map<String, String> VALUE_SETTINGS = Map.of("resistor", "resistance", "capacitor",
            "capacitance", "inductor", "inductance", "diode", "I_saturation", "voltage-source", "U_output",
            "current-source", "I_output");

    /** The component a part is, whether it is written in pin order (a primitive) or by pin name. */
    private static String componentOf(CircuitElement element) {
        return element.type() == ObjectDefinition.CircuitElementType.MODEL ? element.component()
                : element.type().primitiveComponent();
    }

    /** Each pin of the part → its net, however the part is written. */
    private static Map<String, String> terminals(CircuitElement element) {
        if (element.type() == ObjectDefinition.CircuitElementType.MODEL) {
            return element.pins();
        }
        List<String> pins = PRIMITIVE_PINS.get(componentOf(element));
        return Map.of(pins.get(0), element.positive(), pins.get(1), element.negative());
    }

    /** A primitive's value, written as its value or as its value setting; null for any other part. */
    private static String valueOf(CircuitElement element) {
        if (element.type() != ObjectDefinition.CircuitElementType.MODEL) {
            return element.value();
        }
        String setting = VALUE_SETTINGS.get(element.component());
        return setting == null ? null : element.overrides().get(setting);
    }

    private static boolean isInstrument(String component) {
        return component.equals("ammeter") || component.equals("voltmeter");
    }

    // ─── running ────────────────────────────────────────────────────────────────────────────────────

    private static CompiledObject compile(String source) {
        DefinedObject defined = ExperienceDefinitions.read(source, "test");
        return DefinitionCompiler.compile(defined.definition(), defined.parameters(), Map.of(), LIBRARY);
    }

    private static Dataset.TimeSeries simulate(String source) {
        CircuitComputations.TransientResponse transientResponse = new CircuitComputations.TransientResponse();
        ComputationSettings settings = ComputationSettings.resolve(transientResponse.settings(), Map.of("samples", 4001));
        return (Dataset.TimeSeries) transientResponse.compute(compile(source), settings, ComputationProgress.NONE);
    }

    private static float[] column(Dataset.TimeSeries series, String name) {
        int index = series.columns().indexOf(name);
        assertFalse(index < 0, "no column " + name + " in " + series.columns());
        return series.values().get(index);
    }

    private static double last(Dataset.TimeSeries series, String name) {
        float[] values = column(series, name);
        return values[values.length - 1];
    }

    private static double maximumAfter(Dataset.TimeSeries series, String name, double time) {
        float[] values = column(series, name);
        double maximum = Double.NEGATIVE_INFINITY;
        for (int sample = 0; sample < values.length; sample++) {
            if (series.axisValues()[sample] >= time) {
                maximum = Math.max(maximum, values[sample]);
            }
        }
        return maximum;
    }

    private static Map<String, String> catalogue() {
        java.util.regex.Pattern slug = java.util.regex.Pattern.compile("(?m)^[a-z_]+\s+\"[^\"]*\"\s+as\s+([A-Za-z0-9-]+)");
        Map<String, String> documents = new HashMap<>();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(java.nio.file.Path.of(
                SpiceConverterTest.class.getClassLoader().getResource("catalogue").toURI()))) {
            for (java.nio.file.Path file : files.filter(path -> path.toString().endsWith(".jmx")).toList()) {
                String text = java.nio.file.Files.readString(file);
                java.util.regex.Matcher matcher = slug.matcher(text);
                if (matcher.find()) {
                    documents.put(matcher.group(1), text);
                }
            }
        } catch (IOException | java.net.URISyntaxException unreadable) {
            throw new IllegalStateException(unreadable);
        }
        return documents;
    }

    private static Optional<String> resource(String path) {
        try (InputStream stream = SpiceConverterTest.class.getClassLoader().getResourceAsStream(path)) {
            return stream == null ? Optional.empty() : Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
