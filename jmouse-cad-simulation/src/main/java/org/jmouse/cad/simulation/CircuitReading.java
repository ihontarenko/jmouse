package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Analysis;
import org.jmouse.cad.circuit.Bench;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Criterion;
import org.jmouse.cad.circuit.Expectation;
import org.jmouse.cad.circuit.Measure;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.circuit.Probe;
import org.jmouse.cad.circuit.View;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ExtentNode;
import org.jmouse.el.grammar.BlockNode;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a circuit's tree into the model — its parts, where they stand, its probes, its expectations, how it is run — and
 * what a component shares with it: a bench, an expectation.
 */
final class CircuitReading {

    /** The words a measure is conditioned by, each a quantity. */
    private static final List<String> MEASURE_CONDITIONS = List.of("after", "above", "below", "at");

    private CircuitReading() {
    }

    static Circuit circuit(DocumentNode document) {
        List<Probe> probes = new ArrayList<>();
        Formula timeBase = null;
        for (ClauseLineNode line : ModelValues.lines(document, "probes")) {
            if (SimulationLines.TIME_BASE.word().equals(line.word())) {
                timeBase = ModelValues.slotFormula(line, "scale").orElseThrow();
            } else {
                probes.add(probe(line));
            }
        }
        Analysis analysis = document.block("analysis").map(block -> ((ClauseBlockNode) block).header().orElseThrow())
                .map(header -> new Analysis(header.name("kind").orElseThrow(), header.quantity("for").orElseThrow()))
                .orElse(null);
        List<ClauseLineNode> layout = ModelValues.lines(document, "layout");
        String frame = layout.stream().filter(line -> line.grammar() == SimulationLines.FRAME).findFirst()
                .flatMap(line -> line.name("slug")).orElse(null);
        return new Circuit(ModelValues.head(document), ModelValues.parameters(document), parts(ModelValues.lines(document, SimulationVocabulary.PARTS)),
                layout.stream().filter(line -> line.grammar() != SimulationLines.FRAME).map(CircuitReading::placement).toList(),
                probes, timeBase, expectations(document), analysis,
                ModelValues.lines(document, "view").stream().map(CircuitReading::view).toList(), frame, boundary(document));
    }

    /** {@code ports { input in }} and {@code nets { vcc global }}: what the circuit hands across its sheet's edge. */
    private static SheetBoundary boundary(DocumentNode document) {
        List<SheetBoundary.Port> ports = ModelValues.lines(document, "ports").stream()
                .map(line -> new SheetBoundary.Port(line.name("net").orElseThrow(), line.word())).toList();
        List<SheetBoundary.Net> nets = ModelValues.lines(document, "nets").stream()
                .map(line -> new SheetBoundary.Net(line.name("name").orElseThrow(), line.has("ground"), line.has("global"))).toList();
        return new SheetBoundary(ports, nets);
    }

    static List<Part> parts(List<ClauseLineNode> lines) {
        return lines.stream().map(CircuitReading::part).toList();
    }

    private static Part part(ClauseLineNode line) {
        return new Part(line.name("reference").orElseThrow(), line.name("component").orElseThrow(),
                line.slot("connections").map(ModelValues::connections).orElse(List.of()),
                line.slot("settings").map(ModelValues::settings).orElse(List.of()),
                line.word("symbol").orElse(null), line.word("footprint").orElse(null));
    }

    private static Placement placement(ClauseLineNode line) {
        String reference = line.name("reference").orElseThrow();
        double rotation = line.quantity("rotate").orElse(0.0);
        if (line.grammar() == SimulationLines.WIRE) {
            return new Placement.Wire(reference, line.name("from").orElseThrow(), line.word("to").orElseThrow(),
                    line.value("via").map(CircuitReading::corners).orElse(List.of()));
        }
        if (line.grammar() == SimulationLines.MARK) {
            return new Placement.Mark(reference, line.name("symbol").orElseThrow(), line.name("net").orElse(null), ModelValues.point(line, "at").orElseThrow(), rotation);
        }
        // 'U4.B at …' places block B of the part U4 on its own.
        int dot = reference.indexOf('.');
        Point size = line.value("size").map(written -> {
            ExtentNode extent = (ExtentNode) written;
            return new Point(extent.width(), extent.height());
        }).orElse(null);
        List<String> shows = line.value("shows").map(ModelValues::groups).orElse(List.of()).stream().flatMap(List::stream).toList();
        return new Placement.PlacedPart(dot < 0 ? reference : reference.substring(0, dot), dot < 0 ? null : reference.substring(dot + 1),
                ModelValues.point(line, "at").orElseThrow(), rotation, line.has("mirror"),
                line.nested("name").flatMap(name -> ModelValues.point(name, "at")).orElse(null),
                line.nested("value").flatMap(value -> ModelValues.point(value, "at")).orElse(null),
                line.nested("readings").flatMap(readings -> ModelValues.point(readings, "at")).orElse(null), size, shows);
    }

    /** A wire's corners: {@code via 1120 320, 1120 480}, each group its numbers two by two. */
    private static List<Point> corners(Expression via) {
        List<Point> corners = new ArrayList<>();
        for (List<String> group : ModelValues.groups(via)) {
            if (group.size() % 2 != 0) {
                throw new IllegalArgumentException("a wire's corner is 'x y': '" + String.join(" ", group) + "' is not");
            }
            for (int index = 0; index < group.size(); index += 2) {
                corners.add(new Point(Double.parseDouble(group.get(index)), Double.parseDouble(group.get(index + 1))));
            }
        }
        return corners;
    }

    private static Probe probe(ClauseLineNode line) {
        return new Probe(line.name("channel").orElseThrow(), line.name("reading").orElse("voltage"),
                line.name("subject").orElseThrow(), line.name("reference").orElse(null),
                ModelValues.slotFormula(line, "scale").orElse(null), ModelValues.formula(line, "shows").orElse(null));
    }

    private static View view(ClauseLineNode line) {
        return new View(line.name("view").orElseThrow(), line.has("primary"), line.word("parameter").orElse(null),
                ModelValues.formula(line, "from").orElse(null), ModelValues.formula(line, "to").orElse(null),
                ModelValues.formula(line, "points").orElse(null));
    }

    static List<Expectation> expectations(DocumentNode document) {
        return document.blocks("expect").stream().map(block -> expectation((ClauseBlockNode) block)).toList();
    }

    private static Expectation expectation(ClauseBlockNode block) {
        ClauseLineNode header = block.header().orElseThrow();
        Bench bench = null;
        Measure measure = null;
        Criterion criterion = null;
        for (Expression statement : block.getExpressions()) {
            if (statement instanceof ClauseBlockNode nested) {
                bench = bench(nested);
                continue;
            }
            ClauseLineNode line = (ClauseLineNode) statement;
            switch (line.word()) {
                case "measure" -> measure = measure(line);
                case "near" -> criterion = new Criterion.Near(ModelValues.slotFormula(line, "value").orElseThrow(),
                        ModelValues.formula(line.slot("tolerance").orElseThrow()));
                case "within" -> criterion = new Criterion.Within(ModelValues.range(line.slot("range").orElseThrow()));
                case "is" -> criterion = new Criterion.Is(ModelValues.slotFormula(line, "value").orElseThrow());
                default -> throw new IllegalStateException("an expectation holds no '" + line.word() + "'");
            }
        }
        String title = header.name("title").orElseThrow();
        if (measure == null || criterion == null) {
            throw new IllegalArgumentException("the expectation '" + title + "' says what it measures and what it is held to");
        }
        return new Expectation(title, header.word("from").orElse(""), bench, measure, criterion);
    }

    private static Measure measure(ClauseLineNode line) {
        Map<String, Double> conditions = new LinkedHashMap<>();
        MEASURE_CONDITIONS.forEach(word -> line.quantity(word).ifPresent(value -> conditions.put(word, value)));
        String reading = SimulationVocabulary.MEASURED.stream().filter(line::has).findFirst().orElse(null);
        List<String> targets = reading == null ? List.of()
                : line.value(reading).get() instanceof org.jmouse.el.grammar.ListNode ? line.names(reading) : List.of(line.word(reading).orElseThrow());
        return new Measure(line.name("statistic").orElseThrow(), ModelValues.slotFormula(line, "subject").orElse(null), reading,
                targets, conditions, line.value("with").map(ModelValues::settings).orElse(List.of()));
    }

    static Bench bench(BlockNode block) {
        ClauseBlockNode bench = (ClauseBlockNode) block;
        ClauseLineNode header = bench.header().orElseThrow();
        return new Bench(header.name("name").orElse(null), header.quantity("for").orElse(null),
                header.value("at").map(CircuitReading::temperature).orElse(null), header.has("realistic"),
                parts(ModelValues.lines(bench)));
    }

    /** A bench's temperature, kelvin, from one written in °C or K. */
    private static Double temperature(Expression written) {
        Quantity quantity = ((QuantityLiteralNode) written).getQuantity();
        return quantity.kelvin().orElseThrow(() -> new IllegalArgumentException("a bench runs at a temperature, 'at: 25 °C' or 'at: 300 K', not at '" + quantity.written() + "'"));
    }
}
