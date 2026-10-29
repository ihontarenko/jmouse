package org.jmouse.strokefont.node;

import org.jmouse.el.writer.SourceWriter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** One line of a FontoBene file, as the format says it. */
public abstract sealed class FontoBeneLine extends FontoBeneNode {

    protected FontoBeneLine(int line) {
        super(line);
    }

    @Override
    public final void writeSource(SourceWriter writer) {
        writer.verbatim(text());
        writer.endLine();
    }

    /** The line as the format writes it. */
    protected abstract String text();

    /** {@code # …}: words for a reader, anywhere. */
    public static final class Comment extends FontoBeneLine {

        private final String words;

        public Comment(String words, int line) {
            super(line);
            this.words = words;
        }

        public String words() {
            return words;
        }

        @Override
        protected String text() {
            return "#" + words;
        }
    }

    /** {@code [font]}: where the header's entries after it belong. */
    public static final class Section extends FontoBeneLine {

        private final String name;

        public Section(String name, int line) {
            super(line);
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        protected String text() {
            return "[" + name + "]";
        }
    }

    /** {@code letter_spacing = 1.8}: one of the header's entries. */
    public static final class Entry extends FontoBeneLine {

        private final String key;
        private final String value;

        public Entry(String key, String value, int line) {
            super(line);
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }

        @Override
        protected String text() {
            return key + " = " + value;
        }
    }

    /** {@code ---}: the header ends, the glyphs begin. */
    public static final class Separator extends FontoBeneLine {

        public Separator(int line) {
            super(line);
        }

        @Override
        protected String text() {
            return "---";
        }
    }

    /** {@code [0041] A}: a glyph begins, its codepoint and the character as the file shows it. */
    public static final class Glyph extends FontoBeneLine {

        private final int    codepoint;
        private final String shown;

        public Glyph(int codepoint, String shown, int line) {
            super(line);
            this.codepoint = codepoint;
            this.shown = shown;
        }

        public int codepoint() {
            return codepoint;
        }

        @Override
        protected String text() {
            return "[" + String.format(Locale.ROOT, "%04X", codepoint) + "]" + (shown.isEmpty() ? "" : " " + shown);
        }
    }

    /** {@code @0041}: the glyph takes an earlier glyph's lines and trailing space. */
    public static final class Reference extends FontoBeneLine {

        private final int codepoint;

        public Reference(int codepoint, int line) {
            super(line);
            this.codepoint = codepoint;
        }

        public int codepoint() {
            return codepoint;
        }

        @Override
        protected String text() {
            return "@" + String.format(Locale.ROOT, "%04X", codepoint);
        }
    }

    /** {@code ~3.6}: the space after the glyph. */
    public static final class Spacing extends FontoBeneLine {

        private final double trailing;

        public Spacing(double trailing, int line) {
            super(line);
            this.trailing = trailing;
        }

        public double trailing() {
            return trailing;
        }

        @Override
        protected String text() {
            return "~" + number(trailing);
        }
    }

    /**
     * {@code x,y;x,y,bulge;…}: a stroke through its vertices.
     *
     * @param bulge where it is not 0, an arc to the next vertex: its central angle, −9…9 for −180°…180°, counter-clockwise
     *              positive
     */
    public record Vertex(double x, double y, double bulge) {
    }

    public static final class Polyline extends FontoBeneLine {

        private final List<Vertex> vertices;

        public Polyline(List<Vertex> vertices, int line) {
            super(line);
            this.vertices = List.copyOf(vertices);
        }

        public List<Vertex> vertices() {
            return vertices;
        }

        @Override
        protected String text() {
            return String.join(";", vertices.stream().map(vertex -> number(vertex.x()) + "," + number(vertex.y())
                    + (vertex.bulge() == 0 ? "" : "," + number(vertex.bulge()))).toList());
        }
    }

    /** A blank line: the glyph before it ends. */
    public static final class Blank extends FontoBeneLine {

        public Blank(int line) {
            super(line);
        }

        @Override
        protected String text() {
            return "";
        }
    }

    /** A number as the format writes it: as short as it is exact. */
    private static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
