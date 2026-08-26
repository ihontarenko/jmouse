package org.jmouse.access.el;

import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.SourceSpanNode;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;

/**
 * Reads positions and raw text back out of a {@link TokenCursor}, in the policy language's own terms.
 *
 * <p>The reading itself is {@link SourceReading}, shared with every other language on this lexer. What
 * stays here is the part that is {@code .jmp}'s and could not be shared: the span node this parser
 * builds, and the fact that a name in a policy may be any of {@link AccessToken}'s own keywords.</p>
 *
 * <p>⚠️ The keyword set is why these methods exist at all rather than callers reaching for
 * {@link SourceReading} directly. Passing {@code AccessToken.nameTokens()} at ninety call sites is
 * ninety chances to forget it, and forgetting it is a permission or an action that cannot be written
 * down — see {@link AccessToken#nameTokens()}.</p>
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
    public static SourceSpanNode span(TokenCursor cursor) {
        return span(cursor, cursor.current());
    }

    /**
     * Builds a span pointing at a specific token.
     *
     * @param cursor the cursor the token was read from
     * @param token  the token to point at
     * @return a span node carrying that token's line and column
     */
    public static SourceSpanNode span(TokenCursor cursor, Token token) {
        return new SourceSpanNode(token.lineNumber(), column(cursor, token));
    }

    /**
     * Returns the 1-based column of a token, or its raw offset where no source is reachable.
     *
     * @param cursor the cursor the token was read from
     * @param token  the token to locate
     * @return the column within the token's own line
     */
    public static int column(TokenCursor cursor, Token token) {
        return SourceReading.column(cursor, token);
    }

    /**
     * Returns the source text spanning a run of tokens, exactly as it was written.
     *
     * <p>⚠️ Rendering the same run back from its parsed tree would give <em>a</em> spelling of it, not
     * the one in the file. Anything shown back to an administrator has to come from here.</p>
     *
     * @param cursor the cursor the tokens were read from
     * @param first  the first token of the run
     * @param last   the last token of the run, inclusive
     * @return the verbatim text, trimmed of surrounding whitespace
     */
    public static String text(TokenCursor cursor, Token first, Token last) {
        return SourceReading.text(cursor, first, last);
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

    /**
     * Reads a name that may contain hyphens — {@code storage-byte}, {@code parametric-search} — or a
     * quoted string.
     *
     * <p>⚠️ <strong>The lexer does not produce these as one token.</strong> {@code storage-byte}
     * arrives as {@code storage}, {@code -}, {@code byte}, exactly as {@code form:read} arrives as
     * three tokens and is joined by a run of the same kind. Quoting would also work, but a capability
     * key appears on every line of every bundle, and a catalogue in quotes is a catalogue nobody enjoys
     * proofreading — which is the whole reason it moved out of a migration and into a document.</p>
     *
     * <p>The run cannot swallow what follows: nothing that may come after a key — a description, a
     * number, {@code per}, a comma, the end of the line — begins with a hyphen.</p>
     *
     * @param cursor the cursor, positioned on the first token of the name
     * @return the name, hyphens included and quotes removed
     */
    public static String hyphenatedName(TokenCursor cursor) {
        return SourceReading.hyphenatedName(cursor, AccessToken.nameTokens());
    }

    /**
     * Reads a dotted name — {@code entry.listByPurpose} — from the cursor's position.
     *
     * <p>The counterpart of {@link #hyphenatedName(TokenCursor)}, and it exists for the same reason:
     * the lexer gives {@code entry}, {@code .} and {@code listByPurpose} separately, and an action is
     * one word.</p>
     *
     * <p>The run cannot swallow what follows: nothing that may come after an action name — its
     * description, {@code publishes}, the end of the line — begins with a dot.</p>
     *
     * @param cursor the cursor, positioned on the first token of the name
     * @return the name, dots included and quotes removed
     */
    public static String dottedName(TokenCursor cursor) {
        return SourceReading.dottedName(cursor, AccessToken.nameTokens());
    }
}
