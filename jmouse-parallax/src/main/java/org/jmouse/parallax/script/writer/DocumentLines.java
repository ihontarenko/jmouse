package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;
import org.jmouse.units.Quantity;

import java.util.List;
import java.util.stream.Stream;

/**
 * Single lines of a document, written as {@link DocumentWriter} writes them inside their blocks: what an editor that
 * patches a text line by line — a canvas saving what it drew — writes into the line it changes, so a line it leaves
 * alone keeps its comments and alignment while a changed one reads exactly as a whole document would.
 */
public final class DocumentLines {

    private DocumentLines() {
    }

    /** A {@code parameters} line: {@code C = 1 µF in 0.001 .. 10 step 0.001 "Capacitance"}. */
    public static String parameter(Block.Parameter parameter) {
        return DeclarationWriter.parameter(parameter);
    }

    /** A {@code parts} line: {@code R1 resistor (in, out) resistance = R}. */
    public static String part(Block.Part part) {
        return CircuitWriter.part(part);
    }

    /** A {@code layout} line placing a part, a unit or a mark: {@code U4.B at 1500, 2900 rotate 180}. */
    public static String layoutItem(Block.LayoutItem item) {
        return CircuitWriter.item(item);
    }

    /** A {@code layout} line drawing a wire: {@code W1 wire R1.negative to C1.positive via 240 120}. */
    public static String wire(Block.LayoutWire wire) {
        return CircuitWriter.wire(wire);
    }

    /** A {@code nets} line: {@code vcc global}, {@code agnd ground route 300,200 380,200}. */
    public static String net(Block.Net net) {
        return CircuitWriter.net(net);
    }

    /** A {@code ports} line: {@code input VIN}. */
    public static String port(Block.Port port) {
        return CircuitWriter.port(port);
    }

    /** A {@code probes} line: {@code CH1 out against ref 2 V/div}. */
    public static String probe(Block.Probe probe) {
        return CircuitWriter.probe(probe);
    }

    /** The {@code probes} line of the time base: {@code time 5 ms/div}. */
    public static String timeBase(Quantity timePerDivision) {
        return CircuitWriter.timeBase(timePerDivision);
    }

    /** An {@code analysis} statement: {@code analysis transient for 0.01 s}. */
    public static String analysis(Block.Analysis analysis) {
        return CircuitWriter.analysis(analysis);
    }

    /**
     * A drawing's plain statements in the order a symbol document writes them — size, paths, upright paths, pins,
     * labels, indicators —: the lines a drawing canvas redraws; a living drawing's inputs and elements are not among them.
     */
    public static List<String> drawingLines(Block.Symbol drawing) {
        return SymbolWriter.plainLines(drawing);
    }

    /** A footprint's statements in the order its document writes them — its pads, then its outlines —: the lines the footprint editor redraws. */
    public static List<String> footprintLines(Block.Footprint footprint) {
        return Stream.concat(footprint.pads().stream().map(FootprintWriter::pad),
                footprint.outlines().stream().map(FootprintWriter::outline)).toList();
    }
}
