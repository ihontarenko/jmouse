package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.board.FootprintLines;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.svg.path.node.PathData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The copper and the lines of a generated footprint, before they are its document's lines: its pads, then its outlines,
 * in the order they are written.
 */
public record FootprintDrawing(List<Pad> pads, List<Outline> outlines) {

    public FootprintDrawing {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
    }

    /** How a pad is made, as a footprint's line says it. */
    public enum PadType {
        SURFACE("surface"), THROUGH_HOLE("through-hole"), UNPLATED_HOLE("unplated-hole");

        private final String word;

        PadType(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }
    }

    /** The shape a pad is cut in, as a footprint's line says it. */
    public enum PadShape {
        CIRCLE("circle"), RECTANGLE("rectangle"), OVAL("oval"), ROUNDED_RECTANGLE("rounded-rectangle");

        private final String word;

        PadShape(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }
    }

    /**
     * A pad, in millimetres, its centre at (x, y).
     *
     * @param drill     the hole's diameter; 0 for a surface pad
     * @param roundness the share of the shorter side a rounded rectangle's corner is rounded by
     */
    public record Pad(String number, PadType type, PadShape shape, double x, double y, double width, double height,
                      double drill, double roundness) {
    }

    /** A line drawn on a layer: stroked {@code width} wide, or filled. */
    public record Outline(String layer, double width, boolean filled, PathData path) {
    }

    /** The footprint's lines, the notes written above the first as comments. */
    public List<ClauseLineNode> lines(List<String> notes) {
        List<ClauseLineNode> lines = new ArrayList<>();
        for (Pad pad : pads) {
            lines.add(padLine(pad, lines.isEmpty() ? notes : List.of()));
        }
        for (Outline outline : outlines) {
            lines.add(outlineLine(outline, lines.isEmpty() ? notes : List.of()));
        }
        return lines;
    }

    private static ClauseLineNode padLine(Pad pad, List<String> notes) {
        return FootprintLines.pad(new FootprintLines.Pad(pad.number(), pad.type().word(), pad.shape().word(), pad.x(), pad.y(),
                0, pad.width(), pad.height(), pad.drill(), pad.drill(), pad.roundness(), Optional.empty()), notes);
    }

    private static ClauseLineNode outlineLine(Outline outline, List<String> notes) {
        return FootprintLines.outline(new FootprintLines.Outline(outline.layer(), outline.width(), outline.filled(),
                outline.path()), notes);
    }
}
