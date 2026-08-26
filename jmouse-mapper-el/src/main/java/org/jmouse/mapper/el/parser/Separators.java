package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;

/**
 * Skips what separates one construction from the next.
 *
 * <p>A {@code .jmm} file is line-oriented, so the lexer hands back newlines and the parser has to step
 * over them between constructions. ⚠️ It steps over them <em>between</em> constructions only — inside a
 * rule a newline is the terminator that ends the value, and a helper that skipped them everywhere would
 * let one rule run into the next and produce a value nobody wrote.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Separators {

    private Separators() {
    }

    /**
     * Steps over any run of newlines, semicolons and comments.
     *
     * @param cursor the cursor to advance
     */
    public static void skip(TokenCursor cursor) {
        while (cursor.hasNext()) {
            if (cursor.isCurrent(BasicToken.T_SOL, BasicToken.T_NEW_LINE,
                                 BasicToken.T_EOL, BasicToken.T_SEMICOLON)) {
                cursor.next();

                continue;
            }

            if (!skipComment(cursor)) {
                return;
            }
        }
    }

    /**
     * Consumes a {@code #} comment through to the end of its line.
     *
     * <h3>⚠️ Where a trailing comment is actually handled, and it is not here</h3>
     *
     * <p>This covers a comment standing between constructions — a paragraph above a {@code target}
     * block, a line above a rule. A comment at the <em>end</em> of a rule line is not reached by this
     * at all, because a rule's value is read to its terminator before any separator is skipped; that
     * one is handled by {@code #} being one of {@link RuleValueReader}'s terminators, which is what
     * stops it being swallowed into the expression.</p>
     *
     * <p>Two places rather than one, because a {@code .jmm} newline is significant <em>inside</em> a
     * rule and insignificant between rules — the same split that already decides where this helper may
     * be called at all.</p>
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
