package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.SourceText;

import java.util.Optional;
import java.util.Set;

/**
 * A statement of {@code image}: {@code over X in a .. b, Y in c .. d}, {@code start u = …}, {@code stop when …},
 * {@code hit when … crosses 0 [where …]}, {@code value = …}.
 */
final class ImageLineParser extends JmxLineParser<ImageLineParser.Statement> {

    /** One statement of an image. */
    sealed interface Statement {
    }

    record Over(String horizontal, Block.Range horizontalRange, String vertical, Block.Range verticalRange)
            implements Statement {
    }

    record Start(String variable, SourceText value) implements Statement {
    }

    record Stop(SourceText condition) implements Statement {
    }

    record Hit(SourceText crossing, Optional<SourceText> where) implements Statement {
    }

    record Value(SourceText value) implements Statement {
    }

    private final Reading reading;
    private final JmxValues values;

    ImageLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Statement read() {
        TokenCursor cursor = reading.cursor;
        TokenCursor line = reading.lineView();
        Token first = cursor.current();
        return switch (line.valueAt(0)) {
            case "over" -> {
                int comma = line.offsetOf(1, ",");
                if (comma < 0 || !line.checkValueAt(2, "in") || !line.checkValueAt(comma + 2, "in")) {
                    throw reading.refusal(first, "the picture spans two axes: 'over X in a .. b, Y in c .. d'");
                }
                String horizontal = line.valueAt(1);
                String vertical = line.valueAt(comma + 1);
                cursor.shift(3);
                Block.Range horizontalRange = values.range(Set.of());
                cursor.ensure(BasicToken.T_COMMA);
                cursor.shift(2);
                yield new Over(horizontal, horizontalRange, vertical, rest(() -> values.range(Set.of())));
            }
            case "start" -> {
                reading.requireEqualsAhead(2);
                String variable = line.valueAt(1);
                cursor.shift(3);
                yield new Start(variable, rest(() -> values.expression(Set.of())));
            }
            case "stop" -> {
                if (!line.checkValueAt(1, "when")) {
                    throw reading.refusalAhead(1, "a run ends 'stop when condition'");
                }
                cursor.shift(2);
                yield new Stop(rest(() -> values.expression(Set.of())));
            }
            case "hit" -> {
                int crosses = line.offsetOf(2, "crosses");
                if (!line.checkValueAt(1, "when") || crosses < 0 || crosses + 1 >= line.remaining()
                        || !line.checkValueAt(crosses + 1, "0")) {
                    throw reading.refusal(first, "a hit is 'hit when expression crosses 0 [where condition]'");
                }
                cursor.shift(2);
                SourceText crossing = values.expression(Set.of("crosses"));
                cursor.shift(2);
                Optional<SourceText> where = Optional.empty();
                if (reading.cursor.checkValueAt(0, "where")) {
                    cursor.next();
                    where = Optional.of(values.expression(Set.of()));
                }
                reading.requireLineEnd();
                yield new Hit(crossing, where);
            }
            case "value" -> {
                reading.requireEqualsAhead(1);
                cursor.shift(2);
                yield new Value(rest(() -> values.expression(Set.of())));
            }
            default -> throw reading.refusal(first, "an image holds 'over', 'start', 'stop when', 'hit when' and 'value ='");
        };
    }

    /** What a statement reads last, which must end its line. */
    private <T> T rest(java.util.function.Supplier<T> reader) {
        T read = reader.get();
        reading.requireLineEnd();
        return read;
    }

}
