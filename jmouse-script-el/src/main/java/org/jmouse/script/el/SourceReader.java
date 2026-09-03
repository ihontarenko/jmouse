package org.jmouse.script.el;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.ScriptSpanNode;

/**
 * Reads positions and raw text back out of a {@link TokenCursor}, in the script language's own terms.
 *
 * <p>The reading itself is {@link SourceReading}, shared with every other language on this lexer. What
 * stays here is the part that is jMS's and could not be shared: the span node this parser builds, and
 * the fact that a name in a script may be any of {@link ScriptToken}'s own keywords.</p>
 *
 * <p>⚠️ The keyword set is why these methods exist at all rather than callers reaching for
 * {@link SourceReading} directly. Passing {@code ScriptToken.nameTokens()} at forty call sites is
 * forty chances to forget it, and forgetting it is a host event or a facade method that cannot be
 * written down — see {@link ScriptToken#nameTokens()}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceReader {

    private SourceReader() {
    }

    /**
     * Builds a span pointing at the token the cursor currently sits on.
     *
     * @param cursor the cursor to read from
     * @return a span node carrying the token's line and column
     */
    public static ScriptSpanNode span(TokenCursor cursor) {
        return span(cursor, cursor.current());
    }

    /**
     * Builds a span pointing at a specific token.
     *
     * @param cursor the cursor the token was read from
     * @param token  the token to point at
     * @return a span node carrying that token's line and column
     */
    public static ScriptSpanNode span(TokenCursor cursor, Token token) {
        return new ScriptSpanNode(token.lineNumber(), SourceReading.column(cursor, token));
    }

    /**
     * Returns where the cursor currently sits, as the record a failure is reported with.
     *
     * @param cursor the cursor to read from
     * @return the current token's position
     */
    public static SourceSpan at(TokenCursor cursor) {
        return span(cursor).toSourceSpan();
    }

    /**
     * Reads a name, which may be spelled as any keyword of this dialect.
     *
     * @param cursor the cursor, positioned on the name
     * @return the name, quotes removed
     */
    public static String name(TokenCursor cursor) {
        return SourceReading.literal(cursor.ensure(ScriptToken.nameTokens()));
    }

    /**
     * Reads a dotted name — {@code entry.state}, {@code building.owner.id} — from the cursor's position.
     *
     * <p>⚠️ <strong>The lexer does not produce these as one token.</strong> {@code entry.state} arrives
     * as {@code entry}, {@code .}, {@code state}, and is joined by a run of the same kind {@code .jmp}
     * uses for its permissions. The run cannot swallow what follows, because nothing that may come
     * after a property path — an {@code =}, an operator, the end of the line — begins with a dot.</p>
     *
     * @param cursor the cursor, positioned on the first token of the name
     * @return the name, dots included
     */
    public static String propertyPath(TokenCursor cursor) {
        return SourceReading.dottedName(cursor, ScriptToken.nameTokens());
    }

    /**
     * Reads a token's value with any surrounding quotes removed.
     *
     * @param token the token to read
     * @return the token's value, unquoted
     */
    public static String literal(Token token) {
        return SourceReading.literal(token);
    }

}
