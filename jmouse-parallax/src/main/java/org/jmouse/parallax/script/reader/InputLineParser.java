package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Input;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code x in -3 .. 3}, {@code n in 0 .. 500 integer}, {@code (z) in -2 - 2 i .. 2 + 2 i}, {@code (x, y) in -1 .. 1},
 * then {@code step s}, {@code integer}, {@code periodic} and a description in any order.
 */
final class InputLineParser extends JmxLineParser<Input> {

    /** The words an input writes after a bound or a step: where they end. */
    private static final Set<String> WORDS = Set.of("step", "integer", "periodic");

    private final Reading reading;
    private final JmxValues values;

    InputLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Input read() {
        TokenCursor cursor = reading.cursor;
        List<String> names = new ArrayList<>();
        boolean parenthesised = cursor.isCurrent(BasicToken.T_OPEN_PAREN);
        if (parenthesised) {
            Token open = cursor.next();
            while (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
                if (reading.atLineEnd()) {
                    throw reading.refusal(open, "'(' is never closed on this line");
                }
                Token name = cursor.next();
                if (!name.value().equals(",")) {
                    names.add(name.value());
                }
            }
            cursor.next();
        } else {
            names.add(cursor.next().value());
        }
        Optional<Range> range = Optional.empty();
        Optional<SourceText> step = Optional.empty();
        boolean integer = false;
        boolean periodic = false;
        String description = "";
        while (!reading.atLineEnd()) {
            Token word = cursor.current();
            if (reading.isString(word)) {
                description = reading.literal(cursor.next());
            } else if (word.value().equals("in")) {
                cursor.next();
                range = Optional.of(values.range(WORDS));
            } else if (word.value().equals("step")) {
                cursor.next();
                step = Optional.of(values.expression(WORDS));
            } else if (word.value().equals("integer")) {
                cursor.next();
                integer = true;
            } else if (word.value().equals("periodic")) {
                cursor.next();
                periodic = true;
            } else {
                throw reading.refusal(word, "'" + word.value() + "' is not part of an input: expected in, step, integer, "
                        + "periodic or a description");
            }
        }
        return new Input(names, range, step, integer, periodic, parenthesised && names.size() == 1, description);
    }
}
