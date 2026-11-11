package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Drawing;
import org.jmouse.cad.component.Equation;
import org.jmouse.cad.component.Family;
import org.jmouse.cad.component.Figure;
import org.jmouse.cad.component.InternalNode;
import org.jmouse.cad.component.Lead;
import org.jmouse.cad.component.Pin;
import org.jmouse.cad.component.Seat;
import org.jmouse.cad.component.State;
import org.jmouse.el.grammar.ClauseBlockNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.BlockNode;
import org.jmouse.cad.document.DeclarationParser;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.FormulaLineNode;
import org.jmouse.cad.document.ScriptBlockNode;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reads a component's tree into the model, and a family's or a library of functions'. */
final class ComponentReading {

    private ComponentReading() {
    }

    static Component component(DocumentNode document) {
        Map<String, Formula> types = new LinkedHashMap<>();
        document.blocks("type").forEach(block -> {
            ClauseLineNode header = ((ClauseBlockNode) block).header().orElseThrow();
            types.put(header.name("name").orElseThrow(), ModelValues.formula(header.slot("value").orElseThrow()));
        });
        return new Component(ModelValues.head(document), document.declared(DeclarationParser.EXTENDS), document.declared(DeclarationParser.USE),
                document.declared(DeclarationParser.UNITS),
                ModelValues.parameters(document),
                ModelValues.lines(document, "pins").stream().map(ComponentReading::pin).toList(),
                ModelValues.lines(document, "nodes").stream()
                        .map(line -> new InternalNode(line.name("name").orElseThrow(), line.ending().orElse(""))).toList(),
                ModelValues.lines(document, "state").stream().map(ComponentReading::state).toList(),
                ModelValues.lines(document, "display").stream().map(line -> figure(line, false)).toList(),
                ModelValues.lines(document, "measure").stream().map(line -> figure(line, true)).toList(),
                equations(document.block("equations")),
                equations(document.block("formulas")),
                document.block("behaviour").map(block -> ((ScriptBlockNode) block).written()).orElse(null),
                document.block("symbol").map(ComponentReading::drawing).orElse(null),
                document.block("portrait").map(ComponentReading::drawing).orElse(null),
                document.blocks("footprint").stream().map(ComponentReading::seat).toList(),
                types,
                document.block("bench").map(CircuitReading::bench).orElse(null),
                CircuitReading.expectations(document),
                ModelValues.lines(document, "leads").stream().map(line -> new Lead(line.name("pin").orElseThrow(),
                        line.name("colour").orElseThrow(), line.word("stripes").orElse(null))).toList(),
                CircuitReading.parts(ModelValues.lines(document, SimulationVocabulary.PARTS)));
    }

    static Family family(DocumentNode document) {
        return new Family(ModelValues.head(document), document.declared(DeclarationParser.USE), ModelValues.parameters(document),
                equations(document.block("formulas")), equations(document.block("heat")));
    }

    private static Pin pin(ClauseLineNode line) {
        return new Pin(ModelValues.base(line, "name"), ModelValues.index(line, "name"), line.name("nature").orElseThrow(), line.name("direction").orElse(null),
                line.name("drive").orElse(null), line.slot("numbers").map(ModelValues::groups).orElse(List.of()),
                line.has("edge"), line.ending().orElse(""));
    }

    private static State state(ClauseLineNode line) {
        return new State(ModelValues.base(line, "name"), ModelValues.index(line, "name"), ModelValues.given(line.slot("value").orElseThrow()),
                line.name("kind").orElseThrow(), line.value("in").map(ModelValues::range).orElse(null), line.ending().orElse(""));
    }

    private static Figure figure(ClauseLineNode line, boolean reading) {
        return new Figure(ModelValues.base(line, "name"), ModelValues.index(line, "name"), ModelValues.given(line.slot("value").orElseThrow()),
                line.word("unit").orElse(null), reading && line.has("signal"), line.ending().orElse(""));
    }

    private static List<Equation> equations(Optional<BlockNode> block) {
        if (block.isEmpty()) {
            return List.of();
        }
        List<Equation> equations = new ArrayList<>();
        for (Expression statement : block.get().getExpressions()) {
            equations.add(switch (statement) {
                case FormulaLineNode defined -> new Equation.Defined(new Formula(defined.left().written()),
                        new Formula(defined.right().written()), new Formula(defined.statement().written()));
                case ClauseLineNode line when line.word().equals("current") || line.word().equals("voltage") ->
                        new Equation.Branch(line.word(), line.name("from").orElseThrow(), line.name("to").orElseThrow(),
                                ModelValues.formula(line.slot("value").orElseThrow()));
                case ClauseLineNode line -> new Equation.Local(line.name("name").orElseThrow(), line.name("type").orElse(null),
                        ModelValues.formula(line.slot("value").orElseThrow()));
                default -> throw new IllegalStateException("an equation is no '" + statement + "'");
            });
        }
        return equations;
    }

    private static Drawing drawing(BlockNode block) {
        ClauseBlockNode drawing = (ClauseBlockNode) block;
        ClauseLineNode header = drawing.header().orElseThrow();
        List<Drawing.BoxSide> box = new ArrayList<>();
        List<Drawing.Binding> bindings = new ArrayList<>();
        for (ClauseLineNode line : ModelValues.lines(drawing)) {
            if (line.grammar().isNamed()) {
                bindings.add(new Drawing.Binding(line.name("input").orElseThrow(),
                        line.slot("value").map(ModelValues::given).orElse(null), line.name("pin").orElse(null)));
            } else {
                box.add(new Drawing.BoxSide(line.word(), ModelValues.groups(line.slot("pins").orElseThrow()).stream()
                        .map(item -> new Drawing.BoxPin(item.getFirst(), item.subList(1, item.size()))).toList()));
            }
        }
        return new Drawing(header.names("uses"), box, bindings);
    }

    private static Seat seat(BlockNode block) {
        ClauseBlockNode seat = (ClauseBlockNode) block;
        Map<String, String> pads = new LinkedHashMap<>();
        ModelValues.lines(seat).forEach(line -> pads.put(line.name("pad").orElseThrow(), line.name("pin").orElseThrow()));
        return new Seat(seat.header().orElseThrow().name("slug").orElseThrow(), pads);
    }
}
