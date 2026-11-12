package org.jmouse.mesh.vrml;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.mesh.MeshReadException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a VRML97 file into its top-level nodes: each node its type, its {@code DEF} name and its fields; a {@code USE}
 * the node its {@code DEF} made. A field's value is read by what it begins with — numbers, a node, a bracketed list, a
 * word or a text —, so no table of node types is needed to read one. {@code PROTO}, {@code EXTERNPROTO} and
 * {@code ROUTE} make new node types and wire events: a file that uses them is refused, saying which.
 */
public final class VrmlParser {

    private static final String DEFINE = "DEF";
    private static final String USE = "USE";
    private static final String NONE = "NULL";

    private final VrmlLexer lexer = new VrmlLexer();

    /**
     * @param name what the file is called, for a refusal to name
     * @throws MeshReadException at the first thing that does not read
     */
    public List<VrmlNode> parse(String name, String text) {
        Reading reading = new Reading(name, lexer.tokenize(new StringSource(name, text)));
        List<VrmlNode> nodes = new ArrayList<>();
        while (!reading.cursor.isCurrent(VrmlTokenKind.END_OF_TEXT)) {
            VrmlNode node = reading.node();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /** One reading of one file: its cursor and the nodes its {@code DEF}s named so far. */
    private static final class Reading {

        private final String name;
        private final TokenCursor cursor;
        private final Map<String, VrmlNode> defined = new HashMap<>();

        Reading(String name, TokenCursor cursor) {
            this.name = name;
            this.cursor = cursor;
        }

        /** {@code [DEF name] Type { fields }}, {@code USE name} or {@code NULL}; null for NULL. */
        VrmlNode node() {
            Token word = expect(VrmlTokenKind.WORD, "a node");
            switch (word.value()) {
                case DEFINE -> {
                    String defined = expect(VrmlTokenKind.WORD, "the name a DEF gives").value();
                    Token type = expect(VrmlTokenKind.WORD, "the type of the node " + defined + " names");
                    VrmlNode node = body(type.value(), defined);
                    this.defined.put(defined, node);
                    return node;
                }
                case USE -> {
                    Token used = expect(VrmlTokenKind.WORD, "the name a USE takes");
                    VrmlNode node = defined.get(used.value());
                    if (node == null) {
                        throw refusal(used, "USE " + used.value() + " comes before any DEF " + used.value());
                    }
                    return node;
                }
                case NONE -> {
                    return null;
                }
                case "PROTO", "EXTERNPROTO", "ROUTE" ->
                        throw refusal(word, word.value() + " is not read here: only nodes of the standard types are");
                default -> {
                    return body(word.value(), null);
                }
            }
        }

        private VrmlNode body(String type, String defined) {
            expect(VrmlTokenKind.OPEN_BRACE, "'{' opening the " + type + " node");
            Map<String, VrmlValue> fields = new LinkedHashMap<>();
            while (!cursor.isCurrent(VrmlTokenKind.CLOSE_BRACE)) {
                Token field = expect(VrmlTokenKind.WORD, "a field of the " + type + " node, or '}'");
                fields.put(field.value(), value(field));
            }
            cursor.next();
            return new VrmlNode(type, defined, fields);
        }

        /** A field's value, by what it begins with. */
        private VrmlValue value(Token field) {
            Token first = cursor.peek();
            if (first.type() == VrmlTokenKind.OPEN_BRACKET) {
                cursor.next();
                return list(field);
            }
            if (first.type() == VrmlTokenKind.STRING) {
                cursor.next();
                return new VrmlValue.Words(List.of(unquoted(first.value())));
            }
            if (first.type() != VrmlTokenKind.WORD) {
                throw refusal(first, "the field " + field.value() + " has no value");
            }
            if (isNumber(first.value())) {
                Numbers numbers = new Numbers();
                while (cursor.isCurrent(VrmlTokenKind.WORD) && isNumber(cursor.peek().value())) {
                    numbers.add(number(cursor.next()));
                }
                return numbers.value();
            }
            if (startsNode()) {
                return new VrmlValue.Node(node());
            }
            cursor.next();
            return new VrmlValue.Words(List.of(first.value()));
        }

        /** {@code [ … ]}: numbers, nodes or texts, by its first item. */
        private VrmlValue list(Token field) {
            if (cursor.isCurrent(VrmlTokenKind.CLOSE_BRACKET)) {
                cursor.next();
                return new VrmlValue.Numbers(new double[0]);
            }
            Token first = cursor.peek();
            if (first.type() == VrmlTokenKind.WORD && isNumber(first.value())) {
                Numbers numbers = new Numbers();
                while (!cursor.isCurrent(VrmlTokenKind.CLOSE_BRACKET)) {
                    numbers.add(number(expect(VrmlTokenKind.WORD, "a number of " + field.value() + ", or ']'")));
                }
                cursor.next();
                return numbers.value();
            }
            if (first.type() == VrmlTokenKind.STRING) {
                List<String> words = new ArrayList<>();
                while (!cursor.isCurrent(VrmlTokenKind.CLOSE_BRACKET)) {
                    words.add(unquoted(expect(VrmlTokenKind.STRING, "a text of " + field.value() + ", or ']'").value()));
                }
                cursor.next();
                return new VrmlValue.Words(words);
            }
            List<VrmlNode> nodes = new ArrayList<>();
            while (!cursor.isCurrent(VrmlTokenKind.CLOSE_BRACKET)) {
                if (cursor.isCurrent(VrmlTokenKind.END_OF_TEXT)) {
                    throw refusal(field, "the list of " + field.value() + " is opened and never closed");
                }
                VrmlNode node = node();
                if (node != null) {
                    nodes.add(node);
                }
            }
            cursor.next();
            return new VrmlValue.Nodes(nodes);
        }

        /** Whether the word at the cursor begins a node: DEF, USE, NULL, or a type its brace follows. */
        private boolean startsNode() {
            String word = cursor.peek().value();
            return word.equals(DEFINE) || word.equals(USE) || word.equals(NONE) || cursor.isNext(VrmlTokenKind.OPEN_BRACE);
        }

        private Token expect(VrmlTokenKind kind, String what) {
            Token token = cursor.peek();
            if (token.type() == VrmlTokenKind.UNKNOWN) {
                throw refusal(token, "a text in quotes is opened here and never closed");
            }
            if (token.type() != kind) {
                throw refusal(token, token.type() == VrmlTokenKind.END_OF_TEXT ? "the file ends where " + what + " is due"
                        : "'" + token.value() + "' stands where " + what + " is due");
            }
            return cursor.next();
        }

        private double number(Token token) {
            try {
                return Double.parseDouble(token.value());
            } catch (NumberFormatException refused) {
                throw refusal(token, "'" + token.value() + "' is not a number");
            }
        }

        private MeshReadException refusal(Token token, String detail) {
            return new MeshReadException(name + ", line " + token.lineNumber() + ": " + detail);
        }
    }

    /** Numbers as they are read, into an array that grows: a model's point list holds tens of thousands. */
    private static final class Numbers {

        private double[] values = new double[64];
        private int size;

        void add(double value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, size * 2);
            }
            values[size++] = value;
        }

        VrmlValue.Numbers value() {
            return new VrmlValue.Numbers(Arrays.copyOf(values, size));
        }
    }

    /** A number as VRML writes one: a sign, digits, a point, an exponent — {@code -0.5}, {@code .25}, {@code 1e-3}. */
    private static boolean isNumber(String word) {
        char first = word.charAt(0);
        return Character.isDigit(first) || first == '.' || ((first == '-' || first == '+') && word.length() > 1
                && (Character.isDigit(word.charAt(1)) || word.charAt(1) == '.'));
    }

    private static String unquoted(String written) {
        return written.substring(1, written.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
