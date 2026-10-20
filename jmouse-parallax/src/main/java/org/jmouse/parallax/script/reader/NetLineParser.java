package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor.Boundary;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Net;
import org.jmouse.parallax.script.Block.Point;

import java.util.ArrayList;
import java.util.List;

/** {@code name [ground] [global] [route x,y x,y …]}: a net, whether it is ground or global, and the route it is drawn on. */
final class NetLineParser extends JmxLineParser<Net> {

    private static final Boundary COMMA = (token, after, depth) -> token.type() == BasicToken.T_COMMA;

    private final Reading reading;
    private final JmxValues values;

    NetLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Net read() {
        TokenCursor cursor = reading.cursor;
        String name = cursor.next().value();
        boolean ground = false;
        boolean global = false;
        List<Point> route = new ArrayList<>();
        while (!reading.atLineEnd()) {
            Token word = cursor.current();
            if (word.value().equals("ground")) {
                cursor.next();
                ground = true;
            } else if (word.value().equals("global")) {
                cursor.next();
                global = true;
            } else if (word.value().equals("route")) {
                cursor.next();
                while (!reading.atLineEnd() && !cursor.checkValueAt(0, "ground", "global")) {
                    Token at = cursor.current();
                    if (reading.lineView().offsetOf(0, ",") < 0) {
                        throw reading.refusal(at, "a route is 'route x,y x,y …'");
                    }
                    double x = values.quantity(COMMA);
                    cursor.next();
                    Token y = cursor.current();
                    route.add(new Point(x, values.number().orElseThrow(() -> reading.refusal(y,
                            "'" + y.value() + "' must be a number with an optional unit"))));
                }
            } else {
                throw reading.refusal(word, "a net line is 'name [ground] [global] [route x,y …]'");
            }
        }
        return new Net(name, ground, global, route);
    }
}
