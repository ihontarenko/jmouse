package org.jmouse.cad.simulation;

import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Point;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.circuit.Bus;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.Probe;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.ConnectionsNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.el.grammar.SettingsNode;
import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.grammar.Values;
import org.jmouse.el.node.Expression;
import org.jmouse.units.Quantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A circuit's drawn lines as a program makes them — a part, where something stands, a wire, a probe —, the way back from
 * {@link CircuitReading}: what a schematic editor's sheet is written with. Every line is built on the grammar that reads
 * it, and every formula an author wrote — a setting, a probe's scale — is read again by the language from its words.
 */
public final class CircuitLines {

    /** The reading a probe takes when its line names none. */
    private static final String VOLTAGE = "voltage";

    private final DocumentLanguage language;
    private final String           origin;

    /**
     * @param language the language its formulas are read by
     * @param origin   what a refusal names
     */
    public CircuitLines(DocumentLanguage language, String origin) {
        this.language = language;
        this.origin = origin;
    }

    /** What a line of the {@code parts}, {@code layout} or {@code probes} block is known by: its reference, its channel, its word. */
    public static Optional<String> keyOf(Expression statement) {
        if (!(statement instanceof ClauseLineNode line)) {
            return Optional.empty();
        }
        LineGrammar grammar = line.grammar();
        if (grammar == SimulationLines.PART || grammar == SimulationLines.PLACE || grammar == SimulationLines.MARK
                || grammar == SimulationLines.WIRE || grammar == SimulationLines.BUS || grammar == SimulationLines.NOTE || grammar == SimulationLines.DRAWN) {
            return line.name("reference");
        }
        if (grammar == SimulationLines.PROBE) {
            return line.name("channel");
        }
        if (grammar == SimulationLines.FRAME || grammar == SimulationLines.TIME_BASE) {
            return Optional.of(grammar.word());
        }
        if (grammar == SimulationLines.BUS_DECLARATION) {
            return line.name("name").map(name -> SimulationVocabulary.BUS + " " + name);
        }
        if (grammar == ArrangementLines.GROUP_LINE) {
            return line.name("name").map(name -> ArrangementLines.GROUP + " " + name);
        }
        return Optional.empty();
    }

    /** {@code R1 resistor (a = in, b = out) resistance = R [symbol slug] [footprint slug]}. */
    public ClauseLineNode part(Part part) {
        LineBuilder line = LineBuilder.line(SimulationLines.PART).slot("reference", Nodes.name(part.reference()))
                .slot("component", Nodes.word(part.component()));
        if (!part.connections().isEmpty()) {
            line.slot("connections", new ConnectionsNode(part.connections().stream()
                    .map(connection -> new ConnectionsNode.Connection(connection.pin(), connection.net())).toList()));
        }
        if (!part.settings().isEmpty()) {
            line.slot("settings", new SettingsNode(part.settings().stream().map(this::setting).toList()));
        }
        if (part.symbol() != null) {
            line.clause("symbol", Nodes.word(part.symbol()));
        }
        if (part.footprint() != null) {
            line.clause("footprint", Nodes.word(part.footprint()));
        }
        if (!part.pads().isEmpty()) {
            List<ConnectionsNode.Connection> pads = new ArrayList<>();
            part.pads().forEach((pad, pin) -> pads.add(new ConnectionsNode.Connection(pin, pad)));
            line.clause("pads", new ConnectionsNode(pads));
        }
        return line.build();
    }

    /** {@code frame iso-a4-landscape-frame}. */
    public ClauseLineNode frame(String slug) {
        return LineBuilder.line(SimulationLines.FRAME).slot("slug", Nodes.word(slug)).build();
    }

    /** A part where it stands, a mark, or a wire: the {@code layout} block's line for it. */
    public ClauseLineNode placement(Placement placement) {
        LineBuilder line = switch (placement) {
            case Placement.PlacedPart part -> placed(part);
            case Placement.Mark mark -> mark(mark);
            case Placement.Wire wire -> wire(wire);
            case Placement.BusLine bus -> busLine(bus);
            case Placement.Drawn drawn -> LineBuilder.line(SimulationLines.DRAWN).slot("reference", Nodes.name(drawn.reference()))
                    .slot("shape", Nodes.word(drawn.shape().word())).clause("via", cornersOf(drawn.via()));
            case Placement.Note note -> LineBuilder.line(SimulationLines.NOTE).slot("reference", Nodes.name(note.reference()))
                    .slot("words", Nodes.text(note.words())).clause("at", point(note.at()));
        };
        ArrangementLines.write(line, placement.arrangement());
        return line.build();
    }

    /** A group of the layout as its own line: {@code group(front-panel, state: locked)}. */
    public ClauseLineNode group(DrawnGroup group) {
        return ArrangementLines.groupLine(group);
    }

    /** {@code CH1 out 2 V/div}, {@code CH3 current R1 1 mA/div}, {@code CH2 a against b 1 V/div [shows …]}. */
    public ClauseLineNode probe(Probe probe) {
        LineBuilder line = LineBuilder.line(SimulationLines.PROBE).slot("channel", Nodes.name(probe.channel()));
        if (probe.reading() != null && !probe.reading().equals(VOLTAGE)) {
            line.slot("reading", Nodes.word(probe.reading()));
        }
        line.slot("subject", Nodes.word(probe.subject()));
        if (probe.reference() != null) {
            line.slot("reference", Nodes.word(probe.reference()));
        }
        if (probe.scale() != null) {
            line.slot("scale", read(SimulationLines.PROBE, Values.FORMULA, probe.scale()));
        }
        if (probe.shows() != null) {
            line.clause("shows", read(SimulationLines.PROBE, Values.FORMULA, probe.shows()));
        }
        return line.build();
    }

    /** {@code time 5 ms/div}. */
    public ClauseLineNode timeBase(Formula scale) {
        return LineBuilder.line(SimulationLines.TIME_BASE).slot("scale", read(SimulationLines.TIME_BASE, Values.FORMULA, scale))
                .build();
    }

    /** {@code R1 at x, y [rotate r] [mirror] [size w x h] [name at x, y] [value at x, y] [readings at x, y] [shows …]}. */
    private LineBuilder placed(Placement.PlacedPart part) {
        String reference = part.unit() == null ? part.reference() : part.reference() + "." + part.unit();
        LineBuilder line = LineBuilder.line(SimulationLines.PLACE).slot("reference", Nodes.name(reference))
                .clause("at", point(part.at()));
        if (part.rotation() != 0) {
            line.clause("rotate", Nodes.number(part.rotation()));
        }
        if (part.mirrored()) {
            line.flag("mirror");
        }
        if (part.size() != null) {
            line.clause("size", Nodes.extent(part.size().x(), part.size().y()));
        }
        nested(line, "name", part.name());
        nested(line, "value", part.value());
        nested(line, "readings", part.readings());
        if (!part.shows().isEmpty()) {
            line.clause("shows", Nodes.groups(List.of(part.shows())));
        }
        if (!part.ports().isEmpty()) {
            line.clause("ports", portPlaces(part.ports()));
        }
        return line;
    }

    /** {@code in left 60, out right 60}: where a placed sheet's ports stand on its frame. */
    public static Expression portPlaces(List<Placement.PortPlace> ports) {
        return Nodes.groups(ports.stream().map(place -> List.of(place.port(), place.edge().word(), BigDecimal.valueOf(place.along()).stripTrailingZeros().toPlainString())).toList());
    }

    /** {@code GND1 ground [net] at x, y [rotate r]}. */
    private static LineBuilder mark(Placement.Mark mark) {
        LineBuilder line = LineBuilder.line(SimulationLines.MARK).slot("reference", Nodes.name(mark.reference()))
                .slot("symbol", Nodes.word(mark.symbol()));
        if (mark.net() != null) {
            line.slot("net", Nodes.word(mark.net()));
        }
        line.clause("at", point(mark.at()));
        if (mark.rotation() != 0) {
            line.clause("rotate", Nodes.number(mark.rotation()));
        }
        return line;
    }

    /** {@code bus:B1(D, via: [x y, x y …])}: a line of the bus D. */
    private static LineBuilder busLine(Placement.BusLine bus) {
        return LineBuilder.line(SimulationLines.BUS).slot("reference", Nodes.name(bus.reference())).slot("name", Nodes.word(bus.bus()))
                .clause("via", cornersOf(bus.via()));
    }

    /** {@code bus(D[0..7])} — a vector, its nets the language's indexed names —, {@code bus(I2C, members: [SDA, SCL])} — a group. */
    public static ClauseLineNode bus(Bus bus) {
        LineBuilder line = LineBuilder.line(SimulationLines.BUS_DECLARATION);
        bus.numbers().ifPresentOrElse(
                index -> line.slot("name", Nodes.name(bus.name(), index.first(), index.last())),
                () -> line.slot("name", Nodes.name(bus.name())).clause("members", Nodes.groups(bus.members().stream().map(List::of).toList())));
        return line.build();
    }

    /** Points as the language writes a line of them: {@code 240 120, 400 120}. */
    private static org.jmouse.el.grammar.ListNode cornersOf(List<org.jmouse.cad.Point> corners) {
        return Nodes.groups(corners.stream().map(corner -> List.of(Quantity.plain(corner.x()).written(), Quantity.plain(corner.y()).written())).toList());
    }

    /** {@code W1 wire R1.b to C1.a [via x y, x y …]}. */
    private static LineBuilder wire(Placement.Wire wire) {
        LineBuilder line = LineBuilder.line(SimulationLines.WIRE).slot("reference", Nodes.name(wire.reference()))
                .slot("from", Nodes.word(wire.from())).clause("to", Nodes.word(wire.to()));
        if (!wire.via().isEmpty()) {
            line.clause("via", Nodes.groups(wire.via().stream()
                    .map(corner -> List.of(Quantity.plain(corner.x()).written(), Quantity.plain(corner.y()).written())).toList()));
        }
        return line;
    }

    /** {@code name at x, y}: where a part's name, value or readings stand, when they stand elsewhere than the sheet puts them. */
    private static void nested(LineBuilder line, String word, Point at) {
        if (at == null) {
            return;
        }
        LineGrammar grammar = SimulationLines.PLACE.clause(word).nested();
        line.clause(word, LineBuilder.line(grammar).clause("at", point(at)).build().asHeader());
    }

    /** {@code name = formula}, {@code name = "words"}, or a name alone: a flag. */
    private SettingsNode.Setting setting(Setting setting) {
        Expression value = switch (setting.value()) {
            case null -> null;
            case Formula formula -> read(SimulationLines.PART, Values.SETTING, formula);
            case Text text -> Nodes.text(text.text());
        };
        return new SettingsNode.Setting(setting.name(), value);
    }

    private static Expression point(Point point) {
        return Nodes.point(point.x(), point.y());
    }

    private Expression read(LineGrammar line, ValueKind kind, Formula formula) {
        return language.value(line, kind, Set.of(), formula.written(), origin);
    }
}
