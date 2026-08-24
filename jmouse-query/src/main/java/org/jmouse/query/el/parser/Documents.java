package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;

/**
 * The two things every parser in this package does between meaningful tokens.
 *
 * <p>Held here rather than repeated because both are the kind of step that is forgotten in exactly one
 * of five places, and the resulting behaviour — a document that parses everywhere except after a
 * comment, a reserved word refused in {@code view} but silently accepted in {@code function} — is
 * indistinguishable from the language being inconsistent on purpose.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class Documents {

    private Documents() {
    }

    /**
     * Consumes the separators and comment lines between two meaningful tokens.
     *
     * <p>A document may hold any amount of nothing between declarations and between clauses.</p>
     *
     * @param cursor the cursor to advance
     */
    static void skipBlankSpace(TokenCursor cursor) {
        while (cursor.consumeIf(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON) || skipComment(cursor)) {
            // nothing to do — advancing is the whole effect
        }
    }

    /**
     * Refuses a word the language recognises but does not yet accept.
     *
     * <p>⚠️ <strong>This is why reserved words are lexed at all.</strong> Left as identifiers they would
     * parse into whatever came before them and answer something; recognised and refused, they produce a
     * message that says the word is reserved — which is the one thing a reader cannot mistake for a
     * typo of their own.</p>
     *
     * @param cursor the cursor to inspect
     */
    static void refuseReserved(TokenCursor cursor) {
        Token token = cursor.peek();

        if (token.type() instanceof QueryToken keyword && keyword.isReserved()) {
            throw QueryParseException.reserved(keyword, token);
        }
    }

    /**
     * Consumes a {@code #} comment through to the end of its line.
     *
     * @param cursor the cursor to advance
     * @return {@code true} when a comment was there
     */
    private static boolean skipComment(TokenCursor cursor) {
        if (!cursor.isCurrent(BasicToken.T_HASH)) {
            return false;
        }

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            cursor.next();
        }

        return true;
    }
}
