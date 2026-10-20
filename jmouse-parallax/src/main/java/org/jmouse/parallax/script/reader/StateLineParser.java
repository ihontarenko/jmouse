package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.Block.StateVariable;
import org.jmouse.parallax.script.SourceText;

import java.util.Optional;
import java.util.Set;

/** {@code x = 1 continuous "…"}, {@code stored = X discrete in 0 .. 255}, {@code memory[256] = 0 discrete}. */
final class StateLineParser extends JmxLineParser<StateVariable> {

    private static final String SHAPE = "a state is 'name = initial continuous|discrete [\"description\"]'";
    private static final String ROW_SHAPE = "a row of states is 'name[length] = initial discrete', as "
            + "'memory[256] = 0 discrete'";

    /** The words a state writes after its initial value: its nature. */
    private static final Set<String> NATURES = Set.of("continuous", "discrete");

    private static final int LONGEST_ROW = 65536;

    private final Reading reading;
    private final JmxValues values;

    StateLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    StateVariable read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        // A row of states writes its length after its name: the rest of the line reads on as for one state.
        int length = 0;
        int equals = 1;
        if (line.remaining() > 4 && line.checkValueAt(1, "[")) {
            if (!line.valueAt(2).matches("\\d+") || !line.checkValueAt(3, "]")) {
                throw reading.refusal(first, ROW_SHAPE);
            }
            length = Integer.parseInt(line.valueAt(2));
            if (length < 1 || length > LONGEST_ROW) {
                throw reading.refusal(first, "a row of states has 1 to " + LONGEST_ROW + " slots, not " + length);
            }
            equals = 4;
        }
        int nature = line.offsetOf(equals + 1, "continuous", "discrete");
        if (line.remaining() < equals + 3 || !line.checkValueAt(equals, "=") || nature < 0) {
            throw reading.refusal(first, SHAPE);
        }
        if (length > 0 && !line.checkValueAt(nature, "discrete")) {
            throw reading.refusalAhead(nature, "a row of states is discrete: the equations read numbers, not rows");
        }
        String name = cursor.next().value();
        cursor.shift(equals);
        SourceText initial = values.expression(NATURES);
        boolean discrete = cursor.next().value().equals("discrete");
        Optional<Range> range = Optional.empty();
        String description = "";
        if (!reading.atLineEnd() && !reading.isString(cursor.current())) {
            if (!cursor.current().value().equals("in")) {
                throw reading.refusal(cursor.current(), "after continuous or discrete comes 'in minimum .. maximum' or "
                        + "a description");
            }
            cursor.next();
            range = Optional.of(values.range(Set.of()));
        }
        if (!reading.atLineEnd() && reading.isString(cursor.current())) {
            description = reading.literal(cursor.next());
        }
        if (!reading.atLineEnd()) {
            throw reading.refusal(cursor.current(), SHAPE);
        }
        return new StateVariable(name, initial, discrete, range, description, length);
    }
}
