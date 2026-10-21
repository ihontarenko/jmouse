package org.jmouse.kicad.symbol;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.kicad.parser.lexer.KiCadTokenKind;
import org.jmouse.kicad.parser.lexer.KiCadTokenizer;
import org.jmouse.kicad.parser.node.KiCadPosition;
import org.jmouse.kicad.parser.node.KiCadString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a symbol library holds, read for finding rather than for drawing: each symbol's name and its properties —
 * {@code Description}, {@code ki_keywords}, {@code Footprint}… —, walked over the file's tokens without building its
 * nodes. KiCad's own libraries are a couple of hundred megabytes; read whole they would be a heap's worth, and finding
 * a part needs none of their geometry.
 */
public final class KiCadSymbolIndex {

    private static final String SYMBOL = "symbol";
    private static final String PROPERTY = "property";
    /** A symbol of the library sits right under the file's own list, its properties right under it. */
    private static final int SYMBOL_DEPTH = 2;
    private static final int PROPERTY_DEPTH = 3;

    private KiCadSymbolIndex() {
    }

    /**
     * One symbol as an index knows it.
     *
     * @param library    the library's name, the file's without {@code .kicad_sym}
     * @param name       the symbol's name in it
     * @param properties its properties by name, as KiCad writes them
     */
    public record Entry(String library, String name, Map<String, String> properties) {

        public Entry {
            properties = Map.copyOf(properties);
        }

        /** {@code Device:R}: how a schematic names the symbol. */
        public String identifier() {
            return library + ":" + name;
        }

        /** A property's text, or blank. */
        public String property(String property) {
            return properties.getOrDefault(property, "");
        }

        /** What it says of itself: KiCad 8's {@code Description}, an older library's {@code ki_description}. */
        public String description() {
            String description = property("Description");
            return description.isBlank() ? property("ki_description") : description;
        }
    }

    /**
     * The symbols of one library.
     *
     * @param library the library's name
     * @param text    the {@code .kicad_sym} file's text
     */
    public static List<Entry> read(String library, String text) {
        List<Token> tokens = new KiCadTokenizer().tokenize(new StringSource(library, text));
        List<Entry> entries = new ArrayList<>();
        int depth = 0;
        String symbol = null;
        Map<String, String> properties = new LinkedHashMap<>();
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.type() == KiCadTokenKind.OPEN) {
                depth++;
                if (depth == SYMBOL_DEPTH && isHead(tokens, index, SYMBOL)) {
                    symbol = textOf(tokens.get(index + 2));
                    properties = new LinkedHashMap<>();
                } else if (depth == PROPERTY_DEPTH && symbol != null && isHead(tokens, index, PROPERTY)
                        && isString(tokens, index + 3)) {
                    properties.put(textOf(tokens.get(index + 2)), textOf(tokens.get(index + 3)));
                }
            } else if (token.type() == KiCadTokenKind.CLOSE) {
                if (depth == SYMBOL_DEPTH && symbol != null) {
                    entries.add(new Entry(library, symbol, properties));
                    symbol = null;
                }
                depth--;
            }
        }
        return entries;
    }

    /** The list opened at {@code open} is {@code (head "…" …}. */
    private static boolean isHead(List<Token> tokens, int open, String head) {
        return open + 2 < tokens.size() && tokens.get(open + 1).type() == KiCadTokenKind.ATOM
                && tokens.get(open + 1).value().equals(head) && isString(tokens, open + 2);
    }

    private static boolean isString(List<Token> tokens, int index) {
        return index < tokens.size() && tokens.get(index).type() == KiCadTokenKind.STRING;
    }

    private static String textOf(Token token) {
        return KiCadString.read(token.value(), KiCadPosition.NONE, null).text();
    }
}
