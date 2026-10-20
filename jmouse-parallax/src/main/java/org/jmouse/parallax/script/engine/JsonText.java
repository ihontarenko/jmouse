package org.jmouse.parallax.script.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON text and the tree it stands for: objects as ordered maps, arrays as lists, texts, numbers as {@code Double},
 * truth values and null. Small because what it carries is — this module has no JSON library, and a symbol's drawing is
 * the one JSON a definition holds. Writing keeps a map's order and prints every fractional number as a decimal, so two equal trees
 * are equal texts; a whole {@code Integer} or {@code Long} is printed as it is.
 */
final class JsonText {

    private JsonText() {
    }

    /** The tree a JSON text stands for. */
    static Object read(String json) {
        return new Reading(json).document();
    }

    /** A tree as JSON text: maps, lists, texts, numbers, truth values and null. */
    static String write(Object value) {
        StringBuilder json = new StringBuilder();
        write(value, json);
        return json.toString();
    }

    private static void write(Object value, StringBuilder json) {
        switch (value) {
            case null -> json.append("null");
            case Map<?, ?> object -> {
                json.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : object.entrySet()) {
                    if (!first) {
                        json.append(',');
                    }
                    first = false;
                    quote(String.valueOf(entry.getKey()), json);
                    json.append(':');
                    write(entry.getValue(), json);
                }
                json.append('}');
            }
            case List<?> array -> {
                json.append('[');
                for (int index = 0; index < array.size(); index++) {
                    if (index > 0) {
                        json.append(',');
                    }
                    write(array.get(index), json);
                }
                json.append(']');
            }
            case String text -> quote(text, json);
            case Integer whole -> json.append(whole);
            case Long whole -> json.append(whole);
            case Number number -> json.append(number.doubleValue());
            case Boolean truth -> json.append(truth);
            default -> throw new IllegalArgumentException("JSON holds objects, arrays, texts, numbers and truth values, not "
                    + value.getClass().getSimpleName() + ".");
        }
    }

    private static void quote(String text, StringBuilder json) {
        json.append('"');
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    /** One pass over a JSON text. */
    private static final class Reading {

        private final String json;
        private int position;

        Reading(String json) {
            this.json = json;
        }

        Object document() {
            Object value = value();
            skipWhitespace();
            if (position != json.length()) {
                throw refusal("text after the end of the document");
            }
            return value;
        }

        private Object value() {
            skipWhitespace();
            if (position >= json.length()) {
                throw refusal("a value is missing");
            }
            char character = json.charAt(position);
            return switch (character) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> text();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> object = new LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            do {
                skipWhitespace();
                String key = text();
                skipWhitespace();
                expect(':');
                object.put(key, value());
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return object;
        }

        private List<Object> array() {
            List<Object> array = new ArrayList<>();
            position++;
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            do {
                array.add(value());
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return array;
        }

        private String text() {
            expect('"');
            StringBuilder text = new StringBuilder();
            while (position < json.length()) {
                char character = json.charAt(position++);
                if (character == '"') {
                    return text.toString();
                }
                if (character != '\\') {
                    text.append(character);
                    continue;
                }
                if (position >= json.length()) {
                    break;
                }
                char escaped = json.charAt(position++);
                switch (escaped) {
                    case 'n' -> text.append('\n');
                    case 'r' -> text.append('\r');
                    case 't' -> text.append('\t');
                    case 'b' -> text.append('\b');
                    case 'f' -> text.append('\f');
                    case 'u' -> text.append(unicodeEscape());
                    default -> text.append(escaped);
                }
            }
            throw refusal("a text is never closed");
        }

        /** The character of a unicode escape, its four hexadecimal digits next. */
        private char unicodeEscape() {
            if (position + 4 > json.length()) {
                throw refusal("a \\u escape has four hexadecimal digits");
            }
            try {
                char character = (char) Integer.parseInt(json.substring(position, position + 4), 16);
                position += 4;
                return character;
            } catch (NumberFormatException notHexadecimal) {
                throw refusal("a \\u escape has four hexadecimal digits");
            }
        }

        private Double number() {
            int start = position;
            while (position < json.length() && "+-0123456789.eE".indexOf(json.charAt(position)) >= 0) {
                position++;
            }
            if (start == position) {
                throw refusal("'" + json.charAt(position) + "' does not start a value");
            }
            try {
                return Double.parseDouble(json.substring(start, position));
            } catch (NumberFormatException notANumber) {
                throw refusal("'" + json.substring(start, position) + "' is not a number");
            }
        }

        private Object literal(String word, Object value) {
            if (!json.startsWith(word, position)) {
                throw refusal("expected " + word);
            }
            position += word.length();
            return value;
        }

        private void skipWhitespace() {
            while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
                position++;
            }
        }

        private boolean consume(char character) {
            if (position < json.length() && json.charAt(position) == character) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char character) {
            if (!consume(character)) {
                throw refusal("expected '" + character + "'");
            }
        }

        private IllegalArgumentException refusal(String detail) {
            return new IllegalArgumentException("Not valid JSON at " + position + ": " + detail + ".");
        }
    }
}
