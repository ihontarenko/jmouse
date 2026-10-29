package org.jmouse.gerber.excellon.node;

import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * One statement of an Excellon file — a line. The header ({@code M48} … {@code %}) says the format, the unit and the
 * tools; the body selects a tool and drills with it. A statement no node is named for is kept as written
 * ({@link Other}), so nothing read is lost on its way back.
 */
public abstract sealed class ExcellonStatement extends ExcellonNode {

    protected ExcellonStatement(GerberPosition position) {
        super(position);
    }

    /** The statement: its words, then the end of its line. */
    @Override
    public final void writeSource(SourceWriter writer) {
        writeWords(writer);
        writer.endLine();
    }

    /** The words of the statement's line. */
    protected abstract void writeWords(SourceWriter writer);

    /** {@code M48}: the header opens. */
    public static final class HeaderStart extends ExcellonStatement {

        public HeaderStart(GerberPosition position) {
            super(position);
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word("M48");
        }
    }

    /** {@code %}: the header ends, the holes follow. */
    public static final class HeaderEnd extends ExcellonStatement {

        public HeaderEnd(GerberPosition position) {
            super(position);
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word("%");
        }
    }

    /** {@code ; text}: a comment — KiCad's {@code #@! TF.FileFunction,…} attribute among them. */
    public static final class Comment extends ExcellonStatement {

        private final String text;

        public Comment(String text, GerberPosition position) {
            super(position);
            this.text = text.replaceAll("[\\r\\n]", " ").strip();
        }

        public String text() {
            return text;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            // The blank after the semicolon belongs to the comment's look, not to a separator between words.
            writer.word(";").join(" ").join(text);
        }
    }

    /** {@code FMAT,2}: the format version the file is written in. */
    public static final class Format extends ExcellonStatement {

        private final int version;

        public Format(int version, GerberPosition position) {
            super(position);
            this.version = version;
        }

        public int version() {
            return version;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word("FMAT").join(",").number(version);
        }
    }

    /** {@code METRIC} or {@code INCH}, with how zeros are kept when the file says it ({@code TZ}, {@code LZ}). */
    public static final class Unit extends ExcellonStatement {

        private final boolean          metric;
        private final Optional<String> zeros;

        public Unit(boolean metric, Optional<String> zeros, GerberPosition position) {
            super(position);
            this.metric = metric;
            this.zeros = zeros;
        }

        public boolean metric() {
            return metric;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word(metric ? "METRIC" : "INCH");
            zeros.ifPresent(kept -> writer.join(",").word(kept));
        }
    }

    /** {@code T1C0.8}: a tool and its diameter. */
    public static final class ToolDefinition extends ExcellonStatement {

        private final int        tool;
        private final BigDecimal diameter;

        public ToolDefinition(int tool, BigDecimal diameter, GerberPosition position) {
            super(position);
            this.tool = tool;
            this.diameter = diameter;
        }

        public int tool() {
            return tool;
        }

        public BigDecimal diameter() {
            return diameter;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word("T").number(tool).word("C").number(diameter);
        }
    }

    /** {@code T1}: the tool the holes that follow are made with; {@code T0} puts it away. */
    public static final class ToolSelect extends ExcellonStatement {

        private final int tool;

        public ToolSelect(int tool, GerberPosition position) {
            super(position);
            this.tool = tool;
        }

        public int tool() {
            return tool;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word("T").number(tool);
        }
    }

    /** {@code G90}, {@code G05}, {@code M30} …: a code alone on its line, as the format names it. */
    public static final class Code extends ExcellonStatement {

        /** Absolute coordinates. */
        public static final String ABSOLUTE = "G90";
        /** Drilling, not routing. */
        public static final String DRILL = "G05";
        /** The end of the program. */
        public static final String END = "M30";

        private final String word;

        public Code(String word, GerberPosition position) {
            super(position);
            this.word = word;
        }

        public String word() {
            return word;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.word(word);
        }
    }

    /** {@code X1.5Y2}: a round hole drilled at a point; a coordinate left out keeps the last one. */
    public static final class Hole extends ExcellonStatement {

        private final DrillCoordinates at;

        public Hole(DrillCoordinates at, GerberPosition position) {
            super(position);
            this.at = at;
        }

        public DrillCoordinates at() {
            return at;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.write(at);
        }
    }

    /** {@code X1.5Y2G85X3Y2}: a slot routed from a point to a point, its width the tool's. */
    public static final class Slot extends ExcellonStatement {

        private final DrillCoordinates from;
        private final DrillCoordinates to;

        public Slot(DrillCoordinates from, DrillCoordinates to, GerberPosition position) {
            super(position);
            this.from = from;
            this.to = to;
        }

        public DrillCoordinates from() {
            return from;
        }

        public DrillCoordinates to() {
            return to;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            writer.write(from).word("G85").write(to);
        }
    }

    /** A line no node is named for, kept as its words. */
    public static final class Other extends ExcellonStatement {

        private final List<String> words;

        public Other(List<String> words, GerberPosition position) {
            super(position);
            this.words = List.copyOf(words);
        }

        public List<String> words() {
            return words;
        }

        @Override
        protected void writeWords(SourceWriter writer) {
            words.forEach(writer::word);
        }
    }

    /** A point as a drill statement writes it: X and Y in the file's unit, either left out to keep the last. */
    public record DrillCoordinates(Optional<BigDecimal> x, Optional<BigDecimal> y) implements SourceWritable {

        public static DrillCoordinates of(BigDecimal x, BigDecimal y) {
            return new DrillCoordinates(Optional.of(x), Optional.of(y));
        }

        @Override
        public void writeSource(SourceWriter writer) {
            x.ifPresent(value -> writer.word("X").number(value));
            y.ifPresent(value -> writer.word("Y").number(value));
        }
    }
}
