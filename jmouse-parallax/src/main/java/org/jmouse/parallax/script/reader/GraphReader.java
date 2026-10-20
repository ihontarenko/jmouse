package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
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
import java.util.Set;

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
    private final JmxValues values;

    GraphReader(Reading reading, CircuitReader circuits) {
        this.reading = reading;
        this.circuits = circuits;
        this.values = new JmxValues(reading);
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
        List<TokenCursor.Savepoint> starts = reading.lineStarts("expect");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            Token first = reading.cursor.current();
            if (reading.cursor.checkValueAt(0, "measure")) {
                reading.cursor.next();
                measure = reading.atLineEnd() ? reading.text(reading.cursor.endOfRead(), reading.cursor.endOfRead())
                        : values.expression(JmxValues.LINE);
            } else {
                criterion = circuits.criterion();
                written = reading.text(first.offset(), reading.cursor.endOfRead()).text();
            }
        }
        reading.cursor.restore(after);
        if (measure == null || criterion == null) {
            throw reading.refusal(keyword, "expectation '" + reading.literal(title) + "' needs a 'measure' and a criterion (near, within or is)");
        }
        return new Block.GraphCheck(reading.literal(title), measure, criterion, written, reading.lineOf(keyword));
    }

    /** {@code clock t [in a .. b] [speed s] [loop]}. */
    Block.Clock clock(Token keyword) {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        int length = line.remaining();
        if (length == 0) {
            throw reading.refusal(keyword, "a clock names the parameter it moves: 'clock t in 0 .. 1 speed 1 loop'");
        }
        String parameter = line.valueAt(0);
        int end = length;
        boolean loop = line.checkValueAt(end - 1, "loop");
        if (loop) {
            end--;
        }
        int speedAt = line.offsetOf(1, "speed");
        int rangeEnd = speedAt >= 0 && speedAt < end ? speedAt : end;
        Token in = line.lookAt(1);
        int rangeEndAt = line.lookAt(rangeEnd).offset();
        int endAt = line.lookAt(end).offset();
        cursor.next();
        Optional<Range> range = Optional.empty();
        if (rangeEnd > 1) {
            if (!in.value().equals("in") || reading.isString(in)) {
                throw reading.refusal(in, "a clock's range is 'in a .. b'");
            }
            cursor.next();
            range = Optional.of(values.range(JmxValues.at(rangeEndAt)));
        }
        double speed = 1;
        if (speedAt >= 0 && speedAt < end) {
            Token speedWord = cursor.next();
            speed = values.quantity(JmxValues.at(endAt));
            if (!(speed > 0)) {
                throw reading.refusal(speedWord, "a clock's speed is above zero");
            }
        }
        reading.skipLine();
        return new Block.Clock(parameter, range, speed, loop);
    }

    /** {@code panes [rows|columns|grid] { name kind … }}. */
    Block.Panes panes(Token keyword) {
        PaneLayout layout = PaneLayout.ROWS;
        if (!reading.at(BasicToken.T_OPEN_CURLY)) {
            Token word = reading.cursor.next();
            layout = switch (word.value()) {
                case "rows" -> PaneLayout.ROWS;
                case "columns" -> PaneLayout.COLUMNS;
                case "grid" -> PaneLayout.GRID;
                default -> throw reading.refusal(word, "panes are laid out in rows, columns or a grid");
            };
        }
        List<Pane> panes = new ArrayList<>();
        List<TokenCursor.Savepoint> starts = reading.lineStarts("panes");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            Token first = reading.cursor.current();
            Pane pane = pane();
            if (panes.stream().anyMatch(existing -> existing.name().equals(pane.name()))) {
                throw reading.refusal(first, "the pane '" + pane.name() + "' is written twice");
            }
            panes.add(pane);
        }
        reading.cursor.restore(after);
        if (panes.isEmpty()) {
            throw reading.refusal(keyword, "'panes' names at least one pane");
        }
        return new Block.Panes(layout, panes);
    }

    /** {@code name kind …}: a plane's or a space's axes, a circuit pane's circuit and window, a space's camera. */
    private Pane pane() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        if (line.remaining() < 2) {
            throw reading.refusal(first, "a pane is 'name kind …': plane, space, circuit or values");
        }
        PaneKind kind;
        try {
            kind = PaneKind.valueOf(line.valueAt(1).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw reading.refusalAhead(1, "'" + line.valueAt(1) + "' is not a pane: plane, space, circuit or values");
        }
        String name = cursor.next().value();
        cursor.next();
        Map<String, Range> axes = new LinkedHashMap<>();
        Optional<String> uses = Optional.empty();
        Optional<Range> over = Optional.empty();
        Optional<String> camera = Optional.empty();
        while (!reading.atLineEnd()) {
            Token word = cursor.next();
            if (AXES.contains(word.value())) {
                axes.put(word.value(), values.range(PANE_WORDS));
            } else if (word.value().equals("uses") && kind == PaneKind.CIRCUIT) {
                uses = Optional.of(reading.joinedWord());
            } else if (word.value().equals("over") && kind == PaneKind.CIRCUIT) {
                over = Optional.of(values.range(PANE_WORDS));
            } else if (word.value().equals("camera") && kind == PaneKind.SPACE) {
                camera = Optional.of(cursor.next().value());
            } else {
                throw reading.refusal(word, "'" + word.value() + "' is not part of a "
                        + kind.name().toLowerCase(Locale.ROOT) + " pane");
            }
        }
        if (kind == PaneKind.CIRCUIT && uses.isEmpty()) {
            throw reading.refusal(first, "a circuit pane names the circuit it plays: 'uses <slug>'");
        }
        return new Pane(name, kind, axes, uses, over, camera, reading.lineOf(first));
    }

    /** {@code lines { name expression [until u] [at p] [from circuit] [in pane] [color n] [hidden] }}. */
    Block.Lines lines() {
        List<GraphLine> lines = new ArrayList<>();
        List<TokenCursor.Savepoint> starts = reading.lineStarts("lines");
        TokenCursor.Savepoint after = reading.cursor.savepoint();
        for (TokenCursor.Savepoint start : starts) {
            reading.cursor.restore(start);
            Token first = reading.cursor.current();
            GraphLine read = line();
            if (lines.stream().anyMatch(existing -> existing.name().equals(read.name()))) {
                throw reading.refusal(first, "the line '" + read.name() + "' is written twice");
            }
            lines.add(read);
        }
        reading.cursor.restore(after);
        return new Block.Lines(lines);
    }

    /**
     * The words after the expression are found from the end, so an expression may hold anything jMF writes: what is
     * fixed in shape ({@code hidden}, {@code color n}, {@code in pane}) first, then {@code at} and {@code until}, whose
     * expressions run to the next word. Each expression is then read by jMF's grammar up to where its word stands.
     */
    private GraphLine line() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        int length = line.remaining();
        if (length < 2) {
            throw reading.refusal(first, "a line is 'name expression', as 'sum y = sin(x)'");
        }
        int end = length;
        boolean hidden = line.checkValueAt(end - 1, "hidden");
        if (hidden) {
            end--;
        }
        Optional<Integer> color = Optional.empty();
        if (end >= 3 && line.checkValueAt(end - 2, "color")) {
            Token written = line.lookAt(end - 1);
            int number = JmxValues.number(written.value()).map(Double::intValue)
                    .orElseThrow(() -> reading.refusal(first, "a colour is a number, 1 to 6"));
            if (number < 1 || number > 6) {
                throw reading.refusal(written, "a colour is 1 to 6");
            }
            color = Optional.of(number);
            end -= 2;
        }
        Optional<String> pane = Optional.empty();
        if (end >= 4 && line.checkValueAt(end - 2, "in")) {
            pane = Optional.of(line.valueAt(end - 1));
            end -= 2;
        }
        Optional<String> from = Optional.empty();
        if (end >= 4 && line.checkValueAt(end - 2, "from")) {
            from = Optional.of(line.valueAt(end - 1));
            end -= 2;
        }
        int atIndex = lastIndexOf(line, end, "at");
        int atEnd = end;
        if (atIndex > 1) {
            end = atIndex;
        }
        int untilIndex = lastIndexOf(line, end, "until");
        int untilEnd = end;
        if (untilIndex > 1) {
            end = untilIndex;
        }
        // What a graph's line draws is the graph's own language — tuples, a curve's ranges, polygon((0, 0), …) —, not
        // one expression of jMF's: it is cut out as written, between the words found.
        String name = line.valueAt(0);
        Token quote = line.lookAt(1);
        boolean label = reading.isString(quote) && end == 2;
        SourceText quoted = reading.text(quote.offset(), line.lookAt(2).offset());
        SourceText expression = label
                ? new SourceText(reading.literal(quote), quoted.line(), quoted.column(), quoted.offset())
                : reading.text(quote.offset(), line.lookAt(end).offset());
        if (expression.text().isBlank()) {
            throw reading.refusal(first, "the line '" + name + "' has no expression");
        }
        Optional<SourceText> until = untilIndex > 1
                ? Optional.of(reading.text(line.lookAt(untilIndex + 1).offset(), line.lookAt(untilEnd).offset()))
                : Optional.empty();
        Optional<SourceText> at = atIndex > 1
                ? Optional.of(reading.text(line.lookAt(atIndex + 1).offset(), line.lookAt(atEnd).offset()))
                : Optional.empty();
        reading.skipLine();
        return new GraphLine(name, expression, label, until, at, from, pane, color, hidden, reading.lineOf(first));
    }

    /** The last token at depth zero before {@code end} whose word is {@code word}, or -1. */
    private static int lastIndexOf(TokenCursor line, int end, String word) {
        int found = -1;
        int index = line.offsetOf(1, word);
        while (index >= 0 && index < end) {
            found = index;
            index = line.offsetOf(index + 1, word);
        }
        return found;
    }

    /** Where an axis's or a window's range ends: at the next word of a pane. */
    private static final Set<String> PANE_WORDS = Set.of("x", "y", "z", "uses", "over", "camera");
}
