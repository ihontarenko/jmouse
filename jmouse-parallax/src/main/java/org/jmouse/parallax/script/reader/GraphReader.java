package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.GraphLine;
import org.jmouse.parallax.script.Block.Pane;
import org.jmouse.parallax.script.Block.PaneKind;
import org.jmouse.parallax.script.Block.PaneLayout;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The blocks of a {@code graph}: its clock, its panes and its lines.
 *
 * <pre>
 * clock t from 0 to 4 pi speed 1 loop
 * panes rows {
 *     wave   plane x 0 .. 4 pi  y -1 .. 1
 *     sheet  circuit uses bridge-rectifier
 * }
 * lines {
 *     output  y = abs(sin(x))  until t  in wave  color 2
 *     reading "V = {abs(sin(t))}"  at (t, 1)  in wave
 * }
 * </pre>
 */
final class GraphReader {

    private static final List<String> AXES = List.of("x", "y", "z");

    private final Reading reading;
    private final CircuitReader circuits;

    GraphReader(Reading reading, CircuitReader circuits) {
        this.reading = reading;
        this.circuits = circuits;
    }

    /** {@code expect "title" { measure expression;  near … +- … | within … .. … | is … }}: one claim, checked over the clock. */
    Block.GraphCheck check(Token keyword) {
        Token title = reading.cursor.next();
        if (!reading.isString(title)) {
            throw reading.refusal(title, "a graph's expectation is 'expect \"what it claims\" { measure …; near … }'");
        }
        SourceText measure = null;
        Block.Criterion criterion = null;
        String written = null;
        for (Reading.Line line : reading.blockLines("expect")) {
            if (line.value(0).equals("measure")) {
                measure = line.text(1);
            } else {
                criterion = circuits.criterion(line);
                written = line.text(0).text();
            }
        }
        if (measure == null || criterion == null) {
            throw reading.refusal(keyword, "expectation '" + reading.literal(title) + "' needs a 'measure' and a criterion (near, within or is)");
        }
        return new Block.GraphCheck(reading.literal(title), measure, criterion, written, reading.lineOf(keyword));
    }

    /** {@code clock t [from a to b] [speed s] [loop]}. */
    Block.Clock clock(Token keyword) {
        Reading.Line line = reading.line();
        if (line.isEmpty()) {
            throw reading.refusal(keyword, "a clock names the parameter it moves: 'clock t in 0 .. 1 speed 1 loop'");
        }
        String parameter = line.value(0);
        int end = line.size();
        boolean loop = line.value(end - 1).equals("loop");
        if (loop) {
            end--;
        }
        double speed = 1;
        int speedAt = line.indexOf(1, "speed");
        if (speedAt >= 0) {
            speed = Values.quantity(line, speedAt + 1, end);
            if (!(speed > 0)) {
                throw line.refusal(speedAt, "a clock's speed is above zero");
            }
            end = speedAt;
        }
        Optional<Range> range = Optional.empty();
        if (end > 1) {
            if (!line.value(1).equals("in")) {
                throw line.refusal(1, "a clock's range is 'in a .. b'");
            }
            range = Optional.of(Values.range(line, 2, end));
        }
        return new Block.Clock(parameter, range, speed, loop);
    }

    /** {@code panes [rows|columns|grid] { name kind … }}. */
    Block.Panes panes(Token keyword) {
        PaneLayout layout = PaneLayout.ROWS;
        if (!reading.at(org.jmouse.el.lexer.BasicToken.T_OPEN_CURLY)) {
            Token word = reading.cursor.next();
            layout = switch (word.value()) {
                case "rows" -> PaneLayout.ROWS;
                case "columns" -> PaneLayout.COLUMNS;
                case "grid" -> PaneLayout.GRID;
                default -> throw reading.refusal(word, "panes are laid out in rows, columns or a grid");
            };
        }
        List<Pane> panes = new ArrayList<>();
        for (Reading.Line line : reading.blockLines("panes")) {
            Pane pane = pane(line);
            if (panes.stream().anyMatch(existing -> existing.name().equals(pane.name()))) {
                throw line.refusal("the pane '" + pane.name() + "' is written twice");
            }
            panes.add(pane);
        }
        if (panes.isEmpty()) {
            throw reading.refusal(keyword, "'panes' names at least one pane");
        }
        return new Block.Panes(layout, panes);
    }

    private Pane pane(Reading.Line line) {
        if (line.size() < 2) {
            throw line.refusal("a pane is 'name kind …': plane, space, circuit or values");
        }
        PaneKind kind;
        try {
            kind = PaneKind.valueOf(line.value(1).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw line.refusal(1, "'" + line.value(1) + "' is not a pane: plane, space, circuit or values");
        }
        Map<String, Range> axes = new LinkedHashMap<>();
        Optional<String> uses = Optional.empty();
        Optional<Range> over = Optional.empty();
        Optional<String> camera = Optional.empty();
        int index = 2;
        while (index < line.size()) {
            String word = line.value(index);
            if (AXES.contains(word)) {
                int end = nextWord(line, index + 1);
                axes.put(word, Values.range(line, index + 1, end));
                index = end;
            } else if (word.equals("uses") && kind == PaneKind.CIRCUIT) {
                uses = Optional.of(line.joinedWord(index + 1));
                index = line.joinedEnd(index + 1);
            } else if (word.equals("over") && kind == PaneKind.CIRCUIT) {
                int end = nextWord(line, index + 1);
                over = Optional.of(Values.range(line, index + 1, end));
                index = end;
            } else if (word.equals("camera") && kind == PaneKind.SPACE) {
                camera = Optional.of(line.value(index + 1));
                index += 2;
            } else {
                throw line.refusal(index, "'" + word + "' is not part of a " + kind.name().toLowerCase(Locale.ROOT) + " pane");
            }
        }
        if (kind == PaneKind.CIRCUIT && uses.isEmpty()) {
            throw line.refusal("a circuit pane names the circuit it plays: 'uses <slug>'");
        }
        return new Pane(line.value(0), kind, axes, uses, over, camera, reading.lineOf(line.first()));
    }

    /** Where an axis range ends: at the next axis word or the end of the line. */
    private static int nextWord(Reading.Line line, int from) {
        int next = line.indexOf(from, "x", "y", "z", "uses", "over", "camera");
        return next < 0 ? line.size() : next;
    }

    /** {@code lines { name expression [until u] [at p] [from circuit] [in pane] [color n] [hidden] }}. */
    Block.Lines lines() {
        List<GraphLine> lines = new ArrayList<>();
        for (Reading.Line line : reading.blockLines("lines")) {
            GraphLine read = line(line);
            if (lines.stream().anyMatch(existing -> existing.name().equals(read.name()))) {
                throw line.refusal("the line '" + read.name() + "' is written twice");
            }
            lines.add(read);
        }
        return new Block.Lines(lines);
    }

    /**
     * The words after the expression are read from the end, so an expression may hold anything jMF writes: what is
     * fixed in shape ({@code hidden}, {@code color n}, {@code in pane}) first, then {@code at} and {@code until}, whose
     * expressions run to the next word.
     */
    private GraphLine line(Reading.Line line) {
        if (line.size() < 2) {
            throw line.refusal("a line is 'name expression', as 'sum y = sin(x)'");
        }
        int end = line.size();
        boolean hidden = line.value(end - 1).equals("hidden");
        if (hidden) {
            end--;
        }
        Optional<Integer> color = Optional.empty();
        if (end >= 3 && line.value(end - 2).equals("color")) {
            int written = Values.number(line.value(end - 1)).map(Double::intValue)
                    .orElseThrow(() -> line.refusal("a colour is a number, 1 to 6"));
            if (written < 1 || written > 6) {
                throw line.refusal(end - 1, "a colour is 1 to 6");
            }
            color = Optional.of(written);
            end -= 2;
        }
        Optional<String> pane = Optional.empty();
        if (end >= 4 && line.value(end - 2).equals("in")) {
            pane = Optional.of(line.value(end - 1));
            end -= 2;
        }
        Optional<String> from = Optional.empty();
        if (end >= 4 && line.value(end - 2).equals("from")) {
            from = Optional.of(line.value(end - 1));
            end -= 2;
        }
        Optional<SourceText> at = Optional.empty();
        int atIndex = lastIndexOf(line, end, "at");
        if (atIndex > 1) {
            at = Optional.of(line.text(atIndex + 1, end));
            end = atIndex;
        }
        Optional<SourceText> until = Optional.empty();
        int untilIndex = lastIndexOf(line, end, "until");
        if (untilIndex > 1) {
            until = Optional.of(line.text(untilIndex + 1, end));
            end = untilIndex;
        }
        boolean label = line.isString(1) && end == 2;
        SourceText quoted = line.text(1, 2);
        SourceText expression = label ? new SourceText(reading.literal(line.get(1)), quoted.line(), quoted.column(), quoted.offset())
                : line.text(1, end);
        if (expression.text().isBlank()) {
            throw line.refusal("the line '" + line.value(0) + "' has no expression");
        }
        return new GraphLine(line.value(0), expression, label, until, at, from, pane, color, hidden, reading.lineOf(line.first()));
    }

    /** The last token at depth zero before {@code end} whose word is {@code word}, or −1. */
    private static int lastIndexOf(Reading.Line line, int end, String word) {
        int found = -1;
        int index = line.indexOf(1, word);
        while (index >= 0 && index < end) {
            found = index;
            index = line.indexOf(index + 1, word);
        }
        return found;
    }
}
