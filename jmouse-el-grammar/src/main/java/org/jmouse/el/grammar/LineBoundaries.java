package org.jmouse.el.grammar;


import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import java.util.Set;

/**
 * Where a value of a line ends, said for the engine's {@link BoundedTokenCursor}: always at the end of the line, and
 * sooner at what the line's grammar writes next — its next word ({@code at 0, 0 size 1 x 2}), a text in quotes (a pin's
 * label, a path's data). A line's grammar hands its value parser the cursor these give it.
 */
public final class LineBoundaries {

    private LineBoundaries() {
    }

    /** The rest of the line. */
    public static TokenCursor line(TokenCursor cursor) {
        return new BoundedTokenCursor(cursor, Lines.LINE);
    }

    /** Up to one of these words — outside parentheses, never a text in quotes —, or to the line's end. */
    public static TokenCursor beforeWords(TokenCursor cursor, Set<String> words) {
        return new BoundedTokenCursor(cursor, (token, next, depth) -> Lines.LINE.endsAt(token, next, depth)
                || (depth == 0 && token.type() != BasicToken.T_STRING && words.contains(token.value())));
    }

    /** Up to a text in quotes, or to the line's end. */
    public static TokenCursor beforeText(TokenCursor cursor) {
        return beforeWordsOrText(cursor, Set.of());
    }

    /**
     * Up to one of these words or a text in quotes outside brackets, a {@code +-} or the line's end. A word followed by {@code (} is a
     * function an expression calls — {@code log(2)} —, never the grammar's word.
     */
    public static TokenCursor beforeWordsOrText(TokenCursor cursor, Set<String> words) {
        return new BoundedTokenCursor(cursor, (token, next, depth) -> Lines.LINE.endsAt(token, next, depth)
                || depth == 0 && token.type() == BasicToken.T_STRING
                || depth == 0 && plusMinus(token, next)
                || depth == 0 && words.contains(token.value()) && (next == null || next.type() != BasicToken.T_OPEN_PAREN));
    }

    /** {@code +-}, which a document writes for {@code ±}: how far a value may stand from its mark. */
    public static boolean plusMinus(Token token, Token next) {
        return token.type() == BasicToken.T_PLUS && next != null && next.type() == BasicToken.T_MINUS;
    }
}
