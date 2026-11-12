package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What LibrePCB draws on a layer, wherever it is drawn: a symbol, a footprint, a board. Millimetres with Y upwards, angles
 * counter-clockwise in degrees.
 */
public sealed interface LibrePcbGraphic {

    /** The layer it is on: {@code top_legend}, {@code sym_outlines}. */
    String layer();

    /** @param angle how far the side from it to the next corner bends, counter-clockwise; 0 for a straight one */
    record Vertex(LibrePcbPoint at, double angle) {

        static Vertex of(SExpressionList vertex) {
            return new Vertex(LibrePcbValues.position(vertex), LibrePcbValues.number(vertex, LibrePcbKeyword.ANGLE));
        }

        public boolean bends() {
            return angle != 0;
        }

        /** Where the side from this corner to the next stands half way along it, bent as this corner says. */
        public LibrePcbPoint middleTowards(Vertex next) {
            double dx = next.at.x() - at.x();
            double dy = next.at.y() - at.y();
            // Counter-clockwise from here to there bulges to the right of the chord, by half of it times the quarter's tangent.
            double bulge = Math.tan(Math.toRadians(angle) / 4) / 2;
            return new LibrePcbPoint((at.x() + next.at.x()) / 2 + dy * bulge, (at.y() + next.at.y()) / 2 - dx * bulge);
        }
    }

    /** A line of corners, closed when its last corner is its first. */
    record Polygon(String layer, double width, boolean filled, List<Vertex> vertices) implements LibrePcbGraphic {

        public Polygon {
            vertices = List.copyOf(vertices);
        }

        static Polygon of(SExpressionList polygon) {
            return new Polygon(LibrePcbValues.text(polygon, LibrePcbKeyword.LAYER), LibrePcbValues.number(polygon, LibrePcbKeyword.WIDTH),
                    LibrePcbValues.flag(polygon, LibrePcbKeyword.FILL), verticesOf(polygon));
        }
    }

    record Circle(String layer, double width, boolean filled, double diameter, LibrePcbPoint centre) implements LibrePcbGraphic {

        static Circle of(SExpressionList circle) {
            return new Circle(LibrePcbValues.text(circle, LibrePcbKeyword.LAYER), LibrePcbValues.number(circle, LibrePcbKeyword.WIDTH),
                    LibrePcbValues.flag(circle, LibrePcbKeyword.FILL), LibrePcbValues.number(circle, LibrePcbKeyword.DIAMETER),
                    LibrePcbValues.position(circle));
        }
    }

    /** How words stand along their point. */
    enum Alignment {
        LEFT("left"), CENTRE("center"), RIGHT("right");

        private final String word;

        Alignment(String word) {
            this.word = word;
        }

        static Alignment of(String word) {
            for (Alignment alignment : values()) {
                if (alignment.word.equals(word)) {
                    return alignment;
                }
            }
            throw new LibrePcbReadException("'" + word + "' is no way of aligning words.");
        }
    }

    /** How words stand across their point. */
    enum Across {
        TOP("top"), MIDDLE("center"), BOTTOM("bottom");

        private final String word;

        Across(String word) {
            this.word = word;
        }

        static Across of(String word) {
            for (Across across : values()) {
                if (across.word.equals(word)) {
                    return across;
                }
            }
            throw new LibrePcbReadException("'" + word + "' is no way of aligning words.");
        }
    }

    /** @param value what they say; {@code {{NAME}}} and the like stand for what the part or the project has */
    record Text(String layer, double height, LibrePcbPoint at, double rotation, Alignment alignment, Across across, boolean mirrored,
                String value) implements LibrePcbGraphic {

        private static final Pattern LINE_BREAK = Pattern.compile("\\R|\\\\n");

        /** The same words with what they stand for written in. */
        public Text filled(Map<LibrePcbPlaceholder, String> values) {
            return new Text(layer, height, at, rotation, alignment, across, mirrored, LibrePcbPlaceholder.filled(value, values));
        }

        /** What it says line by line: the format writes a break in its words as {@code \\n}. */
        public List<String> lines() {
            return List.of(LINE_BREAK.split(value));
        }

        static Text of(SExpressionList text) {
            return new Text(LibrePcbValues.text(text, LibrePcbKeyword.LAYER), LibrePcbValues.number(text, LibrePcbKeyword.HEIGHT),
                    LibrePcbValues.position(text), LibrePcbValues.numberOr(text, LibrePcbKeyword.ROTATION, 0),
                    Alignment.of(text.requireList(LibrePcbKeyword.ALIGN).word(1)), Across.of(text.requireList(LibrePcbKeyword.ALIGN).word(2)),
                    LibrePcbValues.flag(text, LibrePcbKeyword.MIRROR),
                    LibrePcbValues.text(text, LibrePcbKeyword.VALUE));
        }
    }

    /** The corners a list holds, in order. */
    static List<Vertex> verticesOf(SExpressionList list) {
        return list.lists(LibrePcbKeyword.VERTEX).stream().map(Vertex::of).toList();
    }

    /** Everything a list draws: its polygons, circles and words, stroked or not. */
    static List<LibrePcbGraphic> of(SExpressionList list) {
        List<LibrePcbGraphic> graphics = new ArrayList<>();
        list.lists(LibrePcbKeyword.POLYGON).forEach(polygon -> graphics.add(Polygon.of(polygon)));
        list.lists(LibrePcbKeyword.CIRCLE).forEach(circle -> graphics.add(Circle.of(circle)));
        list.lists(LibrePcbKeyword.STROKE_TEXT).forEach(text -> graphics.add(Text.of(text)));
        list.lists(LibrePcbKeyword.TEXT).forEach(text -> graphics.add(Text.of(text)));
        return graphics;
    }
}
