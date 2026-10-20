package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Output;
import org.jmouse.parallax.script.Block.OutputType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code z : surface = A sin(k x)}, {@code trace : curve = (sin(t), cos(t))}, {@code boundary : surface where …},
 * {@code estimate : series "…"}, {@code mean = mu}.
 */
final class OutputLineParser extends JmxLineParser<Output> {

    private final Reading reading;
    private final JmxValues values;

    OutputLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Output read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        String name = line.valueAt(0);
        int index = 1;
        Optional<OutputType> type = Optional.empty();
        Optional<String> gluing = Optional.empty();
        if (line.checkValueAt(index, ":")) {
            type = Optional.of(outputType(index + 1, line.valueAt(index + 1)));
            index += 2;
            if (line.checkValueAt(index, "glued")) {
                gluing = Optional.of(line.valueAt(index + 1));
                index += 2;
            }
        }
        int length = line.remaining();
        boolean described = length - 1 >= index && line.lookAt(length - 1) != null
                && reading.isString(line.lookAt(length - 1));
        int descriptionAt = described ? length - 1 : length;
        String description = described ? reading.literal(line.lookAt(length - 1)) : "";
        if (index >= descriptionAt) {
            if (type.isEmpty()) {
                throw reading.refusal(first, "output '" + name + "' needs a type ('" + name + " : series') or a value");
            }
            reading.skipLine();
            return new Output(name, type, gluing, List.of(), Optional.empty(), description);
        }
        String word = line.valueAt(index);
        if (word.equals("=")) {
            cursor.shift(index + 1);
            Output output = new Output(name, type, gluing, values.tuple(Set.of()), Optional.empty(), description);
            reading.skipLine();
            return output;
        }
        if (word.equals("where")) {
            cursor.shift(index + 1);
            Output output = new Output(name, type, gluing, List.of(), Optional.of(values.expression(Set.of())), description);
            reading.skipLine();
            return output;
        }
        throw reading.refusalAhead(index, "an output is 'name [: type] = expression', 'name : type where equation' or "
                + "'name : type'");
    }

    private OutputType outputType(int index, String word) {
        try {
            return OutputType.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw reading.refusalAhead(index, "'" + word + "' is not an output type: " + Arrays.stream(OutputType.values())
                    .map(type -> type.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")));
        }
    }
}
