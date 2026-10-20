package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.MeasureFigure;

import java.util.Set;

/** {@code reading = plus - minus unit V [signal]}: what an instrument reads, what that comes out in, and whether a signal. */
final class MeasureLineParser extends JmxLineParser<MeasureLineParser.Measured> {

    /**
     * What a measure line declares.
     *
     * @param first where the line begins, for a refusal of a name measured twice
     */
    record Measured(Token first, String name, MeasureFigure figure) {
    }

    private final Reading reading;
    private final JmxValues values;

    MeasureLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Measured read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        int length = line.remaining();
        int unit = line.offsetOf(2, "unit");
        boolean signal = line.checkValueAt(length - 1, "signal");
        if (length < 3 || !line.checkValueAt(1, "=") || unit < 3 || unit != length - (signal ? 3 : 2)) {
            throw reading.refusal(first, "a measure line is 'name = expression unit U [signal]': "
                    + "'reading = plus - minus unit V signal'");
        }
        String name = cursor.next().value();
        cursor.next();
        var expression = values.expression(Set.of("unit"));
        cursor.next();
        String unitWord = cursor.next().value();
        reading.skipLine();
        return new Measured(first, name, new MeasureFigure(expression, unitWord, signal));
    }
}
