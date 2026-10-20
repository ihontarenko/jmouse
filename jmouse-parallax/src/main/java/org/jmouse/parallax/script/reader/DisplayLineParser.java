package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.SourceText;

import java.util.Optional;
import java.util.Set;

/**
 * A line of {@code display}: {@code name = expression [unit U] ["caption"]} — a figure, written beside the part as well
 * as drawn when it has a unit — or {@code cell[32] = shown ["caption"]}, one figure for each slot of a row state.
 */
final class DisplayLineParser extends JmxLineParser<DisplayLineParser.Figure> {

    private static final String SHAPE = "a display line is 'name = expression [unit U] [\"caption\"]': "
            + "'charge = 100 * (terminal_one - terminal_two) / voltage_rating unit % \"of the rated voltage\"'";
    private static final String ROW_SHAPE = "a row of figures is 'name[count] = row [\"caption\"]': 'cell[32] = shown', "
            + "one figure for each slot";

    /**
     * What a display line declares.
     *
     * @param first   where the line begins, for a refusal about the block as a whole
     * @param count   for a row, how many figures it is; 0 for one figure
     */
    record Figure(Token first, String name, SourceText expression, Optional<String> unit, Optional<String> caption,
                  int count) {
    }

    private final Reading reading;
    private final JmxValues values;

    DisplayLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Figure read() {
        TokenCursor line = reading.lineView();
        return line.checkValueAt(1, "[") ? row(line) : figure(line);
    }

    private Figure figure(TokenCursor line) {
        TokenCursor cursor = reading.cursor;
        Token first = cursor.current();
        int length = line.remaining();
        int unit = line.offsetOf(2, "unit");
        boolean captioned = length > 3 && reading.isString(line.lookAt(length - 1));
        int end = captioned ? length - 1 : length;
        if (length < 3 || !line.checkValueAt(1, "=") || (unit >= 0 && unit != end - 2)) {
            throw reading.refusal(first, SHAPE);
        }
        String name = cursor.next().value();
        cursor.next();
        SourceText expression = values.expression(Set.of("unit"));
        Optional<String> unitWord = Optional.empty();
        if (unit >= 0) {
            cursor.next();
            unitWord = Optional.of(cursor.next().value());
        }
        Optional<String> caption = captioned ? Optional.of(reading.literal(line.lookAt(line.remaining() - 1))) : Optional.empty();
        reading.skipLine();
        return new Figure(first, name, expression, unitWord, caption, 0);
    }

    private Figure row(TokenCursor line) {
        TokenCursor cursor = reading.cursor;
        Token first = cursor.current();
        int length = line.remaining();
        boolean captioned = length == 7 && reading.isString(line.lookAt(6));
        if ((length != 6 && !captioned) || !line.checkValueAt(3, "]") || !line.checkValueAt(4, "=")
                || !line.valueAt(2).matches("\\d+")) {
            throw reading.refusal(first, ROW_SHAPE);
        }
        String name = line.valueAt(0);
        int count = Integer.parseInt(line.valueAt(2));
        Token row = line.lookAt(5);
        SourceText written = reading.text(row.offset(), line.lookAt(6).offset());
        Optional<String> caption = captioned ? Optional.of(reading.literal(line.lookAt(6))) : Optional.empty();
        reading.skipLine();
        return new Figure(first, name, written, Optional.empty(), caption, count);
    }
}
