package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.packages.PackageTable;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.svg.path.node.PathData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The copper and the lines of a generated footprint, before they are its document's lines: its pads, then its outlines,
 * in the order they are written, and the package's body when its table says how high it stands.
 *
 * @param bodyHeight millimetres the package stands over the pcb; empty when its table does not say
 */
public record FootprintDrawing(List<Pad> pads, List<Outline> outlines, OptionalDouble bodyHeight) {

    public FootprintDrawing {
        pads = List.copyOf(pads);
        outlines = List.copyOf(outlines);
    }

    /** A footprint whose table says nothing of its body's height. */
    public FootprintDrawing(List<Pad> pads, List<Outline> outlines) {
        this(pads, outlines, OptionalDouble.empty());
    }

    /** A footprint of a package whose table writes {@code body-height}, when it does. */
    public FootprintDrawing(List<Pad> pads, List<Outline> outlines, PackageTable outline) {
        this(pads, outlines, outline.has(BODY_HEIGHT) ? OptionalDouble.of(outline.length(BODY_HEIGHT)) : OptionalDouble.empty());
    }

    /** The field a package table writes its body's height in: the most it stands over the pcb. */
    private static final String BODY_HEIGHT = "body-height";

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
        bodyHeight.ifPresent(height -> lines.add(FootprintLines.body(height, List.of())));
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
