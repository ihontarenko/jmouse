package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.ArrayList;
import java.util.List;

/** One sheet of a project: the symbols standing on it, the segments of wire between them, and the segments of its bus lines. */
public record LibrePcbSchematic(String name, List<SymbolOnSheet> symbols, List<Segment> segments, List<BusSegment> busSegments) {

    public LibrePcbSchematic {
        symbols = List.copyOf(symbols);
        segments = List.copyOf(segments);
        busSegments = List.copyOf(busSegments);
    }

    /**
     * One symbol of a part on the sheet.
     *
     * @param texts its name and value as this sheet writes them, wherever they were dragged to here
     */
    public record SymbolOnSheet(String id, String part, String gate, LibrePcbTransform transform, List<LibrePcbGraphic.Text> texts) {

        public SymbolOnSheet {
            texts = List.copyOf(texts);
        }
    }

    public record Wire(LibrePcbAnchor from, LibrePcbAnchor to) {
    }

    /** @param labels where the net's name is written on it */
    public record Segment(String net, List<LibrePcbJunction> junctions, List<Wire> wires, List<LibrePcbPoint> labels) {

        public Segment {
            junctions = List.copyOf(junctions);
            wires = List.copyOf(wires);
            labels = List.copyOf(labels);
        }
    }

    /** A piece of a bus line drawn on the sheet: the bus it draws, by id, and its lines between its junctions, as a net segment's. */
    public record BusSegment(String bus, List<LibrePcbJunction> junctions, List<Wire> lines) {

        public BusSegment {
            junctions = List.copyOf(junctions);
            lines = List.copyOf(lines);
        }
    }

    static LibrePcbSchematic of(SExpressionList schematic) {
        List<SymbolOnSheet> symbols = new ArrayList<>();
        for (SExpressionList symbol : schematic.lists(LibrePcbKeyword.SYMBOL)) {
            symbols.add(new SymbolOnSheet(symbol.word(1), LibrePcbValues.text(symbol, LibrePcbKeyword.COMPONENT),
                    LibrePcbValues.text(symbol, LibrePcbKeyword.LIBRARY_GATE), new LibrePcbTransform(LibrePcbValues.position(symbol),
                    LibrePcbValues.number(symbol, LibrePcbKeyword.ROTATION), LibrePcbValues.flag(symbol, LibrePcbKeyword.MIRROR)),
                    symbol.lists(LibrePcbKeyword.TEXT).stream().map(LibrePcbGraphic.Text::of).toList()));
        }
        List<Segment> segments = new ArrayList<>();
        for (SExpressionList segment : schematic.lists(LibrePcbKeyword.SEGMENT)) {
            segments.add(new Segment(LibrePcbValues.text(segment, LibrePcbKeyword.NET),
                    segment.lists(LibrePcbKeyword.JUNCTION).stream().map(LibrePcbJunction::of).toList(),
                    segment.lists(LibrePcbKeyword.LINE).stream().map(LibrePcbSchematic::wire).toList(),
                    segment.lists(LibrePcbKeyword.LABEL).stream().map(LibrePcbValues::position).toList()));
        }
        List<BusSegment> busSegments = new ArrayList<>();
        for (SExpressionList segment : schematic.lists(LibrePcbKeyword.BUS_SEGMENT)) {
            busSegments.add(new BusSegment(LibrePcbValues.text(segment, LibrePcbKeyword.BUS),
                    segment.lists(LibrePcbKeyword.JUNCTION).stream().map(LibrePcbJunction::of).toList(),
                    segment.lists(LibrePcbKeyword.LINE).stream().map(LibrePcbSchematic::wire).toList()));
        }
        return new LibrePcbSchematic(LibrePcbValues.text(schematic, LibrePcbKeyword.NAME), symbols, segments, busSegments);
    }

    private static Wire wire(SExpressionList line) {
        return new Wire(LibrePcbAnchor.of(line.requireList(LibrePcbKeyword.FROM)), LibrePcbAnchor.of(line.requireList(LibrePcbKeyword.TO)));
    }
}
