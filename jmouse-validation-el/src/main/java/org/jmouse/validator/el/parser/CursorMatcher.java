package org.jmouse.validator.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.validator.el.lexer.JmvToken;

/**
 * The one question every jMV parser's {@code supports} has to ask, asked in one place.
 *
 * <h2>⚠️ A keyword is only a keyword where the grammar expects one</h2>
 *
 * <p>A field may legitimately be called {@code when}, {@code gate} or {@code stop}, and the lexer
 * cannot know that — it turns the word into a keyword wherever it appears. So every keyword-led
 * decision asks a second question: is the token after it a {@code :}? A colon means a check line whose
 * field happens to share a keyword's spelling, and the block form is not taken.</p>
 *
 * <p>Written once because the day it is written six times is the day the seventh block forgets it, and
 * a product discovers it may not have a field called {@code always}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CursorMatcher {

    private CursorMatcher() {
    }

    /**
     * Whether the cursor opens the named block, rather than a check line for a field spelled like it.
     *
     * @param cursor  the cursor to test
     * @param keyword the block-opening word
     * @return {@code true} when the block form is meant
     */
    public static boolean opensBlock(TokenCursor cursor, JmvToken keyword) {
        return cursor.isCurrent(keyword) && !cursor.isNext(BasicToken.T_COLON);
    }

    /**
     * Whether the cursor opens a check line — any name at all, followed by a colon.
     *
     * @param cursor the cursor to test
     * @return {@code true} when a check line is meant
     */
    public static boolean opensCheckLine(TokenCursor cursor) {
        return cursor.isCurrent(JmvToken.nameTokens()) && cursor.isNext(BasicToken.T_COLON);
    }
}
