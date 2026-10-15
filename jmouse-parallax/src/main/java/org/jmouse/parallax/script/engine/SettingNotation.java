package org.jmouse.parallax.script.engine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> settings = new LinkedHashMap<>();
        List<String> words = words(written);
        if (words.size() % 2 != 0) {
            throw new IllegalArgumentException("Settings are 'name value' pairs: '" + written + "'.");
        }
        for (int index = 0; index < words.size(); index += 2) {
            settings.put(words.get(index), parse(words.get(index + 1)));
        }
        return settings;
    }

    /** One written value: a list, a quoted text, a truth value, a number or a bare word. */
    static Object parse(String word) {
        String text = word.strip();
        if (text.startsWith("[")) {
            return Notation.items(text, '[', ']', "A list").stream().map(SettingNotation::parse).toList();
        }
        if (text.length() >= 2 && (text.charAt(0) == '"' || text.charAt(0) == '\'')) {
            return text.substring(1, text.length() - 1);
        }
        if (text.equals("true") || text.equals("false")) {
            return Boolean.valueOf(text);
        }
        if (text.matches("[+-]?\\d+")) {
            BigInteger integer = new BigInteger(text);
            if (integer.bitLength() < 32) {
                return integer.intValue();
            }
            return integer.bitLength() < 64 ? (Object) integer.longValue() : integer;
        }
        if (text.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")) {
            return Double.parseDouble(text);
        }
        return text;
    }

    /** The text cut into words: a bracketed list or a quoted text is one word. */
    private static List<String> words(String written) {
        List<String> words = new ArrayList<>();
        int index = 0;
        while (index < written.length()) {
            char character = written.charAt(index);
            if (Character.isWhitespace(character)) {
                index++;
                continue;
            }
            int end = endOfWord(written, index);
            words.add(written.substring(index, end));
            index = end;
        }
        return words;
    }

    private static int endOfWord(String written, int start) {
        char first = written.charAt(start);
        if (first == '"' || first == '\'') {
            int close = written.indexOf(first, start + 1);
            if (close < 0) {
                throw new IllegalArgumentException("A text in quotes is never closed: '" + written + "'.");
            }
            return close + 1;
        }
        if (first == '[') {
            int depth = 0;
            for (int index = start; index < written.length(); index++) {
                if (written.charAt(index) == '[') {
                    depth++;
                } else if (written.charAt(index) == ']' && --depth == 0) {
                    return index + 1;
                }
            }
            throw new IllegalArgumentException("A list is never closed: '" + written + "'.");
        }
        int index = start;
        while (index < written.length() && !Character.isWhitespace(written.charAt(index))) {
            index++;
        }
        return index;
    }
}
