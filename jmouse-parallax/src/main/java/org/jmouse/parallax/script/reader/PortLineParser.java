package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Port;
import org.jmouse.parallax.script.Block.PortDirection;

import java.util.Locale;

/** {@code input|output|power|ground|analog net}: a net a sheet hands to the sheet that places it. */
final class PortLineParser extends JmxLineParser<Port> {

    private final Reading reading;

    PortLineParser(Reading reading) {
        this.reading = reading;
    }

    @Override
    Port read() {
        TokenCursor line = reading.lineView();
        Token first = reading.cursor.current();
        if (line.remaining() != 2) {
            throw reading.refusal(first, "a port is 'input|output|power|ground|analog net'");
        }
        PortDirection direction;
        try {
            direction = PortDirection.valueOf(line.valueAt(0).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw reading.refusal(first, "'" + line.valueAt(0) + "' is not a port direction: input, output, power, ground or analog");
        }
        Port port = new Port(direction, line.valueAt(1));
        reading.skipLine();
        return port;
    }
}
