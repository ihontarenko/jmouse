package org.jmouse.el.lexer.support;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

/**
 * The primitives every construct-matcher is built from. 🔎
 *
 * <p>Dispatch in this expression language is "first parser whose {@code supports} says yes, in priority
 * order", so a matcher that is merely <em>nearly</em> right is a statement quietly handed to the wrong
 * parser. Two rules keep that from happening, and they are properties of the primitives here rather
 * than of any one language:</p>
 *
 * <ul>
 *   <li><strong>Every matcher is anchored.</strong> It reads from the cursor's own position outwards
 *       and never scans the rest of the file for a token it hopes to find. {@link #at} is what makes
 *       that cheap enough to be the habit.</li>
 *   <li><strong>Overlapping shapes are made disjoint, not ordered.</strong> Two constructions that open
 *       the same way are told apart by something one of them additionally requires, so exactly one can
 *       match and priority never has to break the tie.</li>
 * </ul>
 *
 * <h2>⚠️ Mechanism only</h2>
 *
 * <p>Nothing here knows what a construct is called. A language's matchers — the ones named for its own
 * blocks and lines — belong to that language, because a shared module holding one language's grammar is
 * a shared module only one caller can use. This holds what all of them are written <em>with</em>.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CursorLookahead {

    /**
     * How far ahead a {@code ${…}} placeholder may run before it is judged unterminated.
     *
     * <p>Generous for a property path, and short enough that a stray {@code $} cannot make a matcher
     * read the rest of the file.</p>
     */
    public static final int PLACEHOLDER_LOOKAHEAD = 32;

    /** Returned by the scanners here for "this is not that shape". */
    public static final int NO_MATCH = -1;

    private CursorLookahead() {
    }

    /**
     * Reads the token at a lookahead offset, tolerating the end of the stream.
     *
     * @param cursor   the cursor to read from
     * @param offset   the lookahead offset
     * @param expected the types the token may have
     * @return {@code true} when a token is there and has one of those types
     */
    public static boolean at(TokenCursor cursor, int offset, Token.Type... expected) {
        Token token = cursor.lookAt(offset);
        return token != null && cursor.checkAt(offset, expected);
    }

    /**
     * Whether a keyword opens a block — the keyword, then anything up to an opening brace on the same
     * construction.
     *
     * @param cursor  the cursor to read from
     * @param keyword the word the block begins with
     * @param within  how many tokens the header may occupy before the brace
     * @return {@code true} when this is that keyword opening a block
     */
    public static boolean opensBlock(TokenCursor cursor, Token.Type keyword, int within) {
        if (!at(cursor, 0, keyword)) {
            return false;
        }

        for (int index = 1; index <= within; index++) {
            if (at(cursor, index, BasicToken.T_OPEN_CURLY)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Measures a name at a lookahead offset — an identifier, a quoted string, a wildcard, or a
     * placeholder standing in for one.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the name is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    public static int nameLength(TokenCursor cursor, int start) {
        if (at(cursor, start, BasicToken.T_IDENTIFIER, BasicToken.T_STRING, BasicToken.T_MULTIPLY)) {
            return 1;
        }

        return at(cursor, start, BasicToken.T_DOLLAR) ? placeholderLength(cursor, start) : NO_MATCH;
    }

    /**
     * Measures {@code ${…}} from its dollar through its closing brace.
     *
     * @param cursor the cursor to read from
     * @param start  the lookahead offset the placeholder is expected to begin at
     * @return how many tokens it occupies, or {@link #NO_MATCH} when it never closes
     */
    public static int placeholderLength(TokenCursor cursor, int start) {
        if (!at(cursor, start, BasicToken.T_DOLLAR) || !at(cursor, start + 1, BasicToken.T_OPEN_CURLY)) {
            return NO_MATCH;
        }

        for (int index = start + 2; index < start + PLACEHOLDER_LOOKAHEAD; index++) {
            if (at(cursor, index, BasicToken.T_CLOSE_CURLY)) {
                return index - start + 1;
            }

            if (at(cursor, index, BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON)) {
                return NO_MATCH;
            }
        }

        return NO_MATCH;
    }

    /**
     * Measures a name joined by a separator at a lookahead offset — {@code a.b.c}, {@code a-b}.
     *
     * @param cursor     the cursor to read from
     * @param start      the lookahead offset the name is expected to begin at
     * @param separator  the token joining the parts
     * @param nameTokens what may stand as one part of the name
     * @return how many tokens it occupies, or {@link #NO_MATCH}
     */
    public static int joinedNameLength(
            TokenCursor cursor,
            int start,
            Token.Type separator,
            Token.Type... nameTokens
    ) {
        if (!at(cursor, start, nameTokens)) {
            return NO_MATCH;
        }

        int length = 1;

        while (at(cursor, start + length, separator) && at(cursor, start + length + 1, nameTokens)) {
            length += 2;
        }

        return length;
    }
}
