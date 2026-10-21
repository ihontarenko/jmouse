package org.jmouse.parallax.script.engine;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.parallax.script.ExperienceReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Settings as a view writes them — {@code cross_section axis Y offset 0}, {@code gradient_descent start [-1.5, 2]
 * rate 0.0015} — and the values JSON would carry for them.
 *
 * <p>A value keeps its JSON type through the round trip: {@code 30} reads back as an integer and {@code 30.0} as a
 * fraction, a plain name as a text, {@code true} as a truth value, {@code [ … ]} as a list. A text that would read as
 * one of those is written in quotes.</p>
 */
final class SettingNotation {

    private SettingNotation() {
    }

    /** {@code name value name value …}, in the map's order. */
    static String write(Map<String, Object> settings) {
        List<String> pairs = new ArrayList<>();
        settings.forEach((name, value) -> {
            if (!Notation.isName(name)) {
                throw new IllegalArgumentException("A setting is named by a plain name, not '" + name + "'.");
            }
            pairs.add(name + " " + value(value));
        });
        return String.join(" ", pairs);
    }

    private static String value(Object value) {
        return switch (value) {
            case Integer number -> number.toString();
            case Long number -> number.toString();
            case BigInteger number -> number.toString();
            case Number number -> Double.toString(number.doubleValue());
            case Boolean truth -> truth.toString();
            case String text -> isBareWord(text) ? text : Notation.text(text);
            case List<?> items -> "[" + String.join(", ", items.stream().map(SettingNotation::value).toList()) + "]";
            case null -> throw new IllegalArgumentException("A setting has a value; null cannot be written.");
            default -> throw new IllegalArgumentException("A setting is a number, a text, a truth value or a list, not "
                    + value.getClass().getSimpleName() + ".");
        };
    }

    /** Whether a text can stand without quotes and still read back as that text. */
    private static boolean isBareWord(String text) {
        return Notation.isName(text) && !text.equals("true") && !text.equals("false");
    }

    /** The settings written after a representation's name. */
    static Map<String, Object> read(String written) {
        TokenCursor cursor = tokens(written);
        Map<String, Object> settings = new LinkedHashMap<>();
        while (!atEnd(cursor)) {
            String name = SourceReading.touchingWord(cursor, Set.of());
            if (atEnd(cursor)) {
                throw new IllegalArgumentException("Settings are 'name value' pairs: '" + written + "'.");
            }
            settings.put(name, value(cursor, Set.of(), written));
        }
        return settings;
    }

    /** One written value: a list, a quoted text, a truth value, a number or a bare word. */
    static Object parse(String word) {
        TokenCursor cursor = tokens(word);
        Object value = value(cursor, Set.of(), word);
        if (!atEnd(cursor)) {
            throw new IllegalArgumentException("One setting value, not '" + word + "'.");
        }
        return value;
    }

    /** The text as jMX's lexer reads it: a word is what nobody pressed the space bar inside. */
    private static TokenCursor tokens(String written) {
        TokenCursor cursor = LEXER.tokenize(new StringSource("SETTINGS", written));
        cursor.consumeIf(BasicToken.T_SOL);
        return cursor;
    }

    private static boolean atEnd(TokenCursor cursor) {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    /**
     * The value the cursor stands on: {@code [a, b]} item by item, a text in quotes, or one word read by its shape.
     *
     * @param separators what ends a word where it touches it: a list's comma and closing bracket
     */
    private static Object value(TokenCursor cursor, Set<String> separators, String written) {
        if (cursor.isCurrent(BasicToken.T_OPEN_BRACKET)) {
            cursor.next();
            List<Object> items = new ArrayList<>();
            while (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                if (atEnd(cursor)) {
                    throw new IllegalArgumentException("A list is never closed: '" + written + "'.");
                }
                items.add(value(cursor, LIST_SEPARATORS, written));
                if (!cursor.consumeIf(BasicToken.T_COMMA) && !cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                    throw new IllegalArgumentException("A list's items are separated by commas: '" + written + "'.");
                }
            }
            cursor.next();
            return List.copyOf(items);
        }
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            String quoted = cursor.next().value();
            return quoted.substring(1, quoted.length() - 1);
        }
        return word(SourceReading.touchingWord(cursor, separators));
    }

    /** A word by its shape: a whole number keeps its size, a decimal is a fraction, true and false are truth values. */
    private static Object word(String text) {
        if (text.equals("true") || text.equals("false")) {
            return Boolean.valueOf(text);
        }
        if (WHOLE.matcher(text).matches()) {
            BigInteger integer = new BigInteger(text);
            if (integer.bitLength() < 32) {
                return integer.intValue();
            }
            return integer.bitLength() < 64 ? (Object) integer.longValue() : integer;
        }
        if (DECIMAL.matcher(text).matches()) {
            return Double.parseDouble(text);
        }
        return text;
    }

    private static final Pattern WHOLE = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

    /** What ends a word inside a list where it touches it. */
    private static final Set<String> LIST_SEPARATORS = Set.of(",", "]");

    private static final Lexer LEXER = new DefaultLexer(new DefaultTokenizer(
            new ExpressionSplitter(ExperienceReader.DIALECT), new ExpressionRecognizer()));
}
