package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.parallax.script.Block.Parameter;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.parallax.script.Block.Range;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * {@code name = value [in minimum .. maximum | in [a, b, c]] [step s] [log] [integer] [exact] [animate] [live]
 * [per unit] ["description"]} — or {@code name = "text" [in ["one", "other"]] ["description"]}.
 *
 * <p>Every value, bound and step is an expression jMF's grammar reads ({@link JmxValues}); this parser reads the
 * line's own words around them.</p>
 */
final class ParameterLineParser extends JmxLineParser<Parameter> {

    private static final String SHAPE = "a parameter is 'name = value [unit] [in minimum .. maximum] [step s] [flags] "
            + "[\"description\"]'";
    private static final String TEXT_SHAPE = "a text parameter is 'name = \"text\" [in [\"one\", \"other\"]] "
            + "[\"description\"]'";

    /** The flags written as one word, each its {@link ParameterFlag} in lower case; {@code per unit} is two. */
    private static final Set<String> SINGLE_WORD_FLAGS = Set.of("log", "integer", "animate", "live", "exact");

    /** The words a parameter writes after its value: where the value ends. */
    private static final Set<String> WORDS = Set.of("in", "step", "log", "integer", "animate", "live", "per", "exact");

    /** What may follow a range's bounds and its listed values: every word but another {@code in}. */
    private static final Set<String> AFTER_IN = without(WORDS, "in");

    /** What may follow a step: every word but another {@code step}. */
    private static final Set<String> AFTER_STEP = without(WORDS, "step");

    private static Set<String> without(Set<String> words, String word) {
        Set<String> rest = new HashSet<>(words);
        rest.remove(word);
        return Set.copyOf(rest);
    }

    private final Reading reading;
    private final JmxValues values;

    ParameterLineParser(Reading reading, JmxValues values) {
        this.reading = reading;
        this.values = values;
    }

    @Override
    Parameter read() {
        TokenCursor cursor = reading.cursor;
        Token first = cursor.current();
        if (!cursor.isNext(BasicToken.T_EQ) || !cursor.lookAt(1).value().equals("=")) {
            throw reading.refusal(first, SHAPE);
        }
        String name = cursor.next().value();
        cursor.next();
        if (reading.atLineEnd()) {
            throw reading.refusal(first, SHAPE);
        }
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return textParameter(name);
        }
        Values.Written value = values.written(WORDS);
        Optional<Range> range = Optional.empty();
        Optional<SourceText> step = Optional.empty();
        Set<ParameterFlag> flags = EnumSet.noneOf(ParameterFlag.class);
        String description = "";
        List<SourceText> listed = List.of();
        while (!reading.atLineEnd()) {
            Token word = cursor.current();
            if (reading.isString(word)) {
                description = reading.literal(cursor.next());
            } else if (word.value().equals("in")) {
                cursor.next();
                if (cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
                    // `in [1, 3, 7, 19, 49]` — the values a slider stops at, from the first to the last.
                    listed = listed(word);
                    range = Optional.of(new Range(listed.getFirst(), listed.getLast()));
                } else {
                    range = Optional.of(range());
                }
            } else if (word.value().equals("step")) {
                cursor.next();
                step = Optional.of(values.expression(AFTER_STEP));
            } else if (word.value().equals("per") && cursor.lookAt(1).value().equals("unit")) {
                cursor.next();
                cursor.next();
                flags.add(ParameterFlag.PER_UNIT);
            } else if (SINGLE_WORD_FLAGS.contains(word.value())) {
                cursor.next();
                flags.add(ParameterFlag.valueOf(word.value().toUpperCase(Locale.ROOT)));
            } else {
                throw reading.refusal(word, "'" + word.value() + "' is not part of a parameter: expected in, step, log, "
                        + "integer, exact, animate, live, per unit or a description");
            }
        }
        return new Parameter(name, value.expression(), value.quantity(), range, step, flags, description, listed);
    }

    /** {@code minimum .. maximum}, each an expression. */
    private Range range() {
        Token from = reading.cursor.current();
        SourceText minimum = values.expression(AFTER_IN);
        if (!reading.cursor.isCurrent(BasicToken.T_DOUBLE_DOT)) {
            throw reading.refusal(from, "a range is written 'minimum .. maximum'");
        }
        reading.cursor.next();
        return new Range(minimum, values.expression(AFTER_IN));
    }

    /** {@code [a, b, c]}: at least two values, each an expression. */
    private List<SourceText> listed(Token in) {
        TokenCursor cursor = reading.cursor;
        cursor.next();
        List<SourceText> listed = new ArrayList<>();
        listed.add(values.expression(AFTER_IN));
        while (cursor.isCurrent(BasicToken.T_COMMA)) {
            cursor.next();
            listed.add(values.expression(AFTER_IN));
        }
        reading.expect(BasicToken.T_CLOSE_BRACKET, "']' to close the listed values");
        if (listed.size() < 2) {
            throw reading.refusal(in, "a slider over listed values lists at least two: 'in [1, 3, 7]'");
        }
        return listed;
    }

    /**
     * {@code name = "text" [in ["one", "other"]] ["description"]}: a text has no unit, range or step, but may be one of
     * a list — what a part line may write over it, and what an editor offers.
     */
    private Parameter textParameter(String name) {
        TokenCursor cursor = reading.cursor;
        Token text = cursor.next();
        SourceText written = reading.text(text.offset(), reading.endOfRead());
        List<SourceText> choices = List.of();
        if (!reading.atLineEnd() && cursor.current().value().equals("in")) {
            Token in = cursor.next();
            if (!cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
                throw reading.refusal(in, TEXT_SHAPE + ": its choices are a list of texts in brackets");
            }
            Token open = cursor.next();
            choices = new ArrayList<>();
            while (true) {
                if (reading.atLineEnd()) {
                    throw reading.refusal(open, TEXT_SHAPE + ": the list of choices is not closed");
                }
                Token choice = cursor.current();
                if (!reading.isString(choice)) {
                    throw reading.refusal(choice, TEXT_SHAPE + ": each choice is one text in quotes");
                }
                cursor.next();
                choices.add(reading.text(choice.offset(), reading.endOfRead()));
                if (cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                    cursor.next();
                    break;
                }
                if (!cursor.isCurrent(BasicToken.T_COMMA)) {
                    throw reading.refusal(cursor.current(), TEXT_SHAPE + ": each choice is one text in quotes");
                }
                cursor.next();
            }
            if (choices.size() < 2) {
                throw reading.refusal(in, TEXT_SHAPE + ": a list of choices names at least two");
            }
            if (choices.stream().noneMatch(choice -> choice.text().equals(written.text()))) {
                throw reading.refusal(text, written.text() + " is not one of its own choices");
            }
        }
        String description = "";
        if (!reading.atLineEnd() && cursor.isCurrent(BasicToken.T_STRING)) {
            description = reading.literal(cursor.next());
        }
        if (!reading.atLineEnd()) {
            throw reading.refusal(cursor.current(), TEXT_SHAPE + ": a text has no unit, range or step");
        }
        return new Parameter(name, written, Optional.empty(), Optional.empty(), Optional.empty(),
                EnumSet.of(ParameterFlag.TEXT), description, choices);
    }
}
