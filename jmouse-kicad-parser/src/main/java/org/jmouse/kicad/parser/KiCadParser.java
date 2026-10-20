package org.jmouse.kicad.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.kicad.parser.lexer.KiCadLexer;
import org.jmouse.kicad.parser.lexer.KiCadTokenKind;
import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;
import org.jmouse.kicad.parser.node.KiCadPosition;
import org.jmouse.kicad.parser.node.KiCadString;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a KiCad file — a schematic, a symbol library, a board — into its S-expression tree, losing nothing: every node
 * keeps the blanks written before it, so {@link KiCadPrinter} gives an untouched file back byte for byte. A file is one
 * root list; anything after it but blanks is refused, with where it stands. Stateless and safe to share.
 */
public final class KiCadParser {

    private final KiCadLexer lexer = new KiCadLexer();

    /**
     * The file's tree.
     *
     * @param name what the file is called, for a refusal to name
     * @throws KiCadParseException at the first thing that does not read
     */
    public KiCadDocument parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        Reading reading = new Reading(name, text, cursor);
        if (!cursor.isCurrent(KiCadTokenKind.OPEN)) {
            throw reading.refusal(cursor.peek(), "a KiCad file is one list: it begins with '('");
        }
        KiCadList root = reading.list();
        if (!cursor.isCurrent(KiCadTokenKind.END_OF_TEXT)) {
            throw reading.refusal(cursor.peek(), "a KiCad file is one list, and this follows it");
        }
        return new KiCadDocument(name, root, text.substring(reading.consumed));
    }

    /** One reading of one text: the cursor, and how far into the text the tokens read so far reach. */
    private static final class Reading {

        private final String name;
        private final String text;
        private final TokenCursor cursor;
        private final TokenizableSource source;
        private int consumed;

        Reading(String name, String text, TokenCursor cursor) {
            this.name = name;
            this.text = text;
            this.cursor = cursor;
            this.source = cursor.source();
        }

        /** A list, the cursor at its opening parenthesis; the blanks before each node kept with it. */
        KiCadList list() {
            Token open = cursor.next();
            String leading = blanksBefore(open);
            List<KiCadNode> items = new ArrayList<>();
            while (!cursor.isCurrent(KiCadTokenKind.CLOSE)) {
                Token token = cursor.peek();
                switch ((KiCadTokenKind) token.type()) {
                    case OPEN -> items.add(list());
                    case ATOM -> {
                        cursor.next();
                        items.add(new KiCadAtom(token.value(), positionOf(token), blanksBefore(token)));
                    }
                    case STRING -> {
                        cursor.next();
                        items.add(KiCadString.read(token.value(), positionOf(token), blanksBefore(token)));
                    }
                    case UNKNOWN -> throw refusal(token, "a text in quotes is opened here and never closed");
                    case END_OF_TEXT -> throw refusal(open, "this list is opened and never closed");
                    default -> throw refusal(token, "'" + token.value() + "' has no place here");
                }
            }
            Token close = cursor.next();
            String closingLeading = blanksBefore(close);
            return new KiCadList(items, closingLeading, positionOf(open), leading);
        }

        /** What stands between the last token read and this one: the blanks written before it. */
        private String blanksBefore(Token token) {
            String blanks = text.substring(consumed, token.offset());
            consumed = token.offset() + token.value().length();
            return blanks;
        }

        private KiCadPosition positionOf(Token token) {
            return new KiCadPosition(token.lineNumber(), source.getColumnNumber(token.offset()));
        }

        KiCadParseException refusal(Token token, String detail) {
            return new KiCadParseException(name, positionOf(token), detail);
        }
    }
}
