package org.jmouse.cad.pcb;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.document.PathTextNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.LineBuilder;
import org.jmouse.el.grammar.ListNode;
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
                      double height, double drillWidth, double drillHeight, double roundness, Optional<PathData> outline,
                      String face, String opens, Double maskMargin, Double pasteMargin) {

        /** A pad that opens what its type does, by the pcb's own margins. */
        public Pad(String number, String type, String shape, double x, double y, double rotation, double width, double height,
                   double drillWidth, double drillHeight, double roundness, Optional<PathData> outline) {
            this(number, type, shape, x, y, rotation, width, height, drillWidth, drillHeight, roundness, outline, "", "", null, null);
        }
    }

    /** A line drawn on a layer — {@code silkscreen}, {@code fabrication}, {@code courtyard}, {@code copper} —: stroked {@code width} wide, or filled. */
    public record Outline(String layer, double width, boolean filled, PathData path) {
    }

    /** {@code pad "1" surface rectangle at x, y [rotate r] size w x h [drill d | drill w x h] [roundness r] ["outline"]}. */
    public static ClauseLineNode pad(Pad pad, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.PAD)
                .slot(PcbWords.NUMBER, Nodes.text(pad.number()))
                .slot(PcbWords.TYPE, Nodes.word(pad.type()))
                .slot(PcbWords.SHAPE, Nodes.word(pad.shape()))
                .clause(PcbWords.AT, Nodes.point(pad.x(), pad.y()));
        if (pad.rotation() != 0) {
            line.clause(PcbWords.ROTATE, Nodes.number(pad.rotation()));
        }
        line.clause(PcbWords.SIZE, Nodes.extent(pad.width(), pad.height()));
        if (pad.drillWidth() > 0) {
            boolean round = pad.drillHeight() == 0 || pad.drillHeight() == pad.drillWidth();
            line.clause(PcbWords.DRILL, round ? Nodes.number(pad.drillWidth()) : Nodes.extent(pad.drillWidth(), pad.drillHeight()));
        }
        if (pad.shape().equals(ROUNDED_RECTANGLE) && pad.roundness() > 0) {
            line.clause(PcbWords.ROUNDNESS, Nodes.number(pad.roundness()));
        }
        if (pad.face() != null && !pad.face().isEmpty()) {
            line.clause(PcbWords.FACE, Nodes.word(pad.face()));
        }
        if (pad.opens() != null && !pad.opens().isEmpty()) {
            line.clause(PcbWords.OPENS, Nodes.word(pad.opens()));
        }
        if (pad.maskMargin() != null) {
            line.clause(PcbWords.MASK_MARGIN, Nodes.number(pad.maskMargin()));
        }
        if (pad.pasteMargin() != null) {
            line.clause(PcbWords.PASTE_MARGIN, Nodes.number(pad.pasteMargin()));
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
     * @param anchor   which part of the letters stands on the point ({@link PcbVocabulary#TEXT_ANCHORS}); null for their
     *                 middle, the usual
     */
    public record Field(String subject, double x, double y, double height, double rotation, @Nullable String layer, @Nullable String anchor) {

        /** A field standing on its middle. */
        public Field(String subject, double x, double y, double height, double rotation, String layer) {
            this(subject, x, y, height, rotation, layer, null);
        }
    }

    /** {@code outline LAYER [width w] [filled] "path"}. */
    public static ClauseLineNode outline(Outline outline, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.OUTLINE).slot(PcbWords.LAYER, Nodes.word(outline.layer()));
        if (outline.width() > 0) {
            line.clause(PcbWords.WIDTH, Nodes.number(outline.width()));
        }
        if (outline.filled()) {
            line.flag(PcbWords.FILLED);
        }
        comments.forEach(line::comment);
        return line.ending(new PathTextNode(outline.path())).build();
    }

    /** {@code body height h}: how high the package stands over the pcb, in millimetres. */
    public static ClauseLineNode body(double height, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.BODY).clause(PcbWords.HEIGHT, Nodes.number(height));
        comments.forEach(line::comment);
        return line.build();
    }

    /** Three values, one for each axis. */
    public record Axes(double x, double y, double z) {

        public static final Axes ZERO = new Axes(0, 0, 0);
        public static final Axes ONE = new Axes(1, 1, 1);

        private ListNode written() {
            return Nodes.numbers(List.of(x, y, z));
        }
    }

    /**
     * A 3D model set onto the footprint ({@link FootprintLinesParser#MODEL}).
     *
     * @param reference the model document's slug
     * @param hidden    whether it is kept but not shown
     */
    public record Model(String reference, Axes offset, Axes rotation, Axes scale, boolean hidden) {
    }

    /** {@code model "MODEL" [offset x, y, z] [rotate x, y, z] [scale x, y, z] [hidden]}, each clause only when it moves something. */
    public static ClauseLineNode model(Model model, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.MODEL).slot(PcbWords.REFERENCE, Nodes.text(model.reference()));
        if (!model.offset().equals(Axes.ZERO)) {
            line.clause(PcbWords.OFFSET, model.offset().written());
        }
        if (!model.rotation().equals(Axes.ZERO)) {
            line.clause(PcbWords.ROTATE, model.rotation().written());
        }
        if (!model.scale().equals(Axes.ONE)) {
            line.clause(PcbWords.SCALE, model.scale().written());
        }
        if (model.hidden()) {
            line.flag(PcbWords.HIDDEN);
        }
        comments.forEach(line::comment);
        return line.build();
    }

    /** {@code field reference|value at x, y size height [rotate r] [layer silkscreen|fabrication] [anchor start|end]}. */
    public static ClauseLineNode field(Field field, List<String> comments) {
        LineBuilder line = LineBuilder.line(FootprintLinesParser.FIELD).slot(PcbWords.SUBJECT, Nodes.word(field.subject()))
                .clause(PcbWords.AT, Nodes.point(field.x(), field.y()))
                .clause(PcbWords.SIZE, Nodes.number(field.height()));
        if (field.rotation() != 0) {
            line.clause(PcbWords.ROTATE, Nodes.number(field.rotation()));
        }
        if (field.layer() != null) {
            line.clause(PcbWords.LAYER, Nodes.word(field.layer()));
        }
        if (field.anchor() != null && !PcbVocabulary.TEXT_MIDDLE.equals(field.anchor())) {
            line.clause(PcbWords.ANCHOR, Nodes.word(field.anchor()));
        }
        comments.forEach(line::comment);
        return line.build();
    }
}
