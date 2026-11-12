package org.jmouse.sexpression;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.sexpression.lexer.SExpressionLexer;
import org.jmouse.sexpression.lexer.SExpressionTokenKind;
import org.jmouse.sexpression.node.SExpressionAtom;
import org.jmouse.sexpression.node.SExpressionList;
import org.jmouse.sexpression.node.SExpressionNode;
import org.jmouse.sexpression.node.SExpressionPosition;
import org.jmouse.sexpression.node.SExpressionString;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads an S-expression file — a schematic, a symbol library, a board — into its S-expression tree, losing nothing: every node
 * keeps the blanks written before it, so {@link SExpressionPrinter} gives an untouched file back byte for byte. A file is one
 * root list; anything after it but blanks is refused, with where it stands. Stateless and safe to share.
 */
public final class SExpressionParser {

    private final SExpressionLexer lexer = new SExpressionLexer();

    /**
     * The file's tree.
     *
     * @param name what the file is called, for a refusal to name
     * @throws SExpressionParseException at the first thing that does not read
     */
    public SExpressionDocument parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        Reading reading = new Reading(name, text, cursor);
        if (!cursor.isCurrent(SExpressionTokenKind.OPEN)) {
            throw reading.refusal(cursor.peek(), "an S-expression file is one list: it begins with '('");
        }
        SExpressionList root = reading.list();
        if (!cursor.isCurrent(SExpressionTokenKind.END_OF_TEXT)) {
            throw reading.refusal(cursor.peek(), "an S-expression file is one list, and this follows it");
        }
        return new SExpressionDocument(name, root, text.substring(reading.consumed));
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
        SExpressionList list() {
            Token open = cursor.next();
            String leading = blanksBefore(open);
            List<SExpressionNode> items = new ArrayList<>();
            while (!cursor.isCurrent(SExpressionTokenKind.CLOSE)) {
                Token token = cursor.peek();
                switch ((SExpressionTokenKind) token.type()) {
                    case OPEN -> items.add(list());
                    case ATOM -> {
                        cursor.next();
                        items.add(new SExpressionAtom(token.value(), positionOf(token), blanksBefore(token)));
                    }
                    case STRING -> {
                        cursor.next();
                        items.add(SExpressionString.read(token.value(), positionOf(token), blanksBefore(token)));
                    }
                    case UNKNOWN -> throw refusal(token, "a text in quotes is opened here and never closed");
                    case END_OF_TEXT -> throw refusal(open, "this list is opened and never closed");
                    default -> throw refusal(token, "'" + token.value() + "' has no place here");
                }
            }
            Token close = cursor.next();
            String closingLeading = blanksBefore(close);
            return new SExpressionList(items, closingLeading, positionOf(open), leading);
        }

        /** What stands between the last token read and this one: the blanks written before it. */
        private String blanksBefore(Token token) {
            String blanks = text.substring(consumed, token.offset());
            consumed = token.offset() + token.value().length();
            return blanks;
        }

        private SExpressionPosition positionOf(Token token) {
            return new SExpressionPosition(token.lineNumber(), source.getColumnNumber(token.offset()));
        }

        SExpressionParseException refusal(Token token, String detail) {
            return new SExpressionParseException(name, positionOf(token), detail);
        }
    }
}
