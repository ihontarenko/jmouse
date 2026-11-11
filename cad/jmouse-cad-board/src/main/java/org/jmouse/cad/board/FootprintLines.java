package org.jmouse.cad.board;

import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.svg.path.node.PathData;

import java.util.List;
import java.util.Optional;

/**
 * A footprint's lines as a program makes them — a pad, an outline —, built on the grammars that read them
 * ({@link FootprintLinesParser}), so whatever makes a footprint writes it as a footprint is read: a generated package
 * and one converted from KiCad alike.
 */
public final class FootprintLines {

    /** The shape whose corners a pad's roundness rounds. */
    private static final String ROUNDED_RECTANGLE = "rounded-rectangle";

    private FootprintLines() {
    }

    /**
     * A pad, in millimetres, its centre at (x, y).
     *
     * @param type        {@code through-hole}, {@code surface}, {@code connector}, {@code unplated-hole}
     * @param shape       {@code circle}, {@code rectangle}, {@code oval}, {@code trapezoid}, {@code rounded-rectangle},
     *                    {@code custom}
     * @param rotation    degrees; 0 for none
     * @param drillWidth  the hole's width — its diameter, for a round one —; 0 for a surface pad
     * @param drillHeight the hole's height, when it is a slot; 0, or the width, for a round one
     * @param roundness   the share of the shorter side a rounded rectangle's corner is rounded by
     * @param outline     a custom pad's own outline, from its centre; empty for any other
     */
    public record Pad(String number, String type, String shape, double x, double y, double rotation, double width,
                      double height, double drillWidth, double drillHeight, double roundness, Optional<PathData> outline) {
    }

    /** A line drawn on a layer — {@code silkscreen}, {@code fabrication}, {@code courtyard}, {@code copper} —: stroked {@code width} wide, or filled. */
    public record Outline(String layer, double width, boolean filled, PathData path) {
    }

    /** {@code pad "1" surface rectangle at x, y [rotate r] size w x h [drill d | drill w x h] [roundness r] ["outline"]}. */
    public static ClauseLineNode pad(Pad pad, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.PAD)
                .slot("number", Nodes.text(pad.number()))
                .slot("type", Nodes.word(pad.type()))
                .slot("shape", Nodes.word(pad.shape()))
                .clause("at", Nodes.point(pad.x(), pad.y()));
        if (pad.rotation() != 0) {
            line.clause("rotate", Nodes.number(pad.rotation()));
        }
        line.clause("size", Nodes.extent(pad.width(), pad.height()));
        if (pad.drillWidth() > 0) {
            boolean round = pad.drillHeight() == 0 || pad.drillHeight() == pad.drillWidth();
            line.clause("drill", round ? Nodes.number(pad.drillWidth()) : Nodes.extent(pad.drillWidth(), pad.drillHeight()));
        }
        if (pad.shape().equals(ROUNDED_RECTANGLE) && pad.roundness() > 0) {
            line.clause("roundness", Nodes.number(pad.roundness()));
        }
        pad.outline().ifPresent(outline -> line.ending(new PathTextNode(outline)));
        comments.forEach(line::comment);
        return line.build();
    }

    /**
     * Where a part's reference or value is printed, its text {@code height} high.
     *
     * @param subject  {@code reference} or {@code value}
     * @param rotation degrees; 0 for none
     * @param layer    {@code silkscreen} or {@code fabrication}; null for where a field is printed when none is named
     */
    public record Field(String subject, double x, double y, double height, double rotation, String layer) {
    }

    /** {@code outline LAYER [width w] [filled] "path"}. */
    public static ClauseLineNode outline(Outline outline, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.OUTLINE).slot("layer", Nodes.word(outline.layer()));
        if (outline.width() > 0) {
            line.clause("width", Nodes.number(outline.width()));
        }
        if (outline.filled()) {
            line.flag("filled");
        }
        comments.forEach(line::comment);
        return line.ending(new PathTextNode(outline.path())).build();
    }

    /** {@code field reference|value at x, y size height [rotate r] [layer silkscreen|fabrication]}. */
    public static ClauseLineNode field(Field field, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.FIELD).slot("subject", Nodes.word(field.subject()))
                .clause("at", Nodes.point(field.x(), field.y()))
                .clause("size", Nodes.number(field.height()));
        if (field.rotation() != 0) {
            line.clause("rotate", Nodes.number(field.rotation()));
        }
        if (field.layer() != null) {
            line.clause("layer", Nodes.word(field.layer()));
        }
        comments.forEach(line::comment);
        return line.build();
    }
}
