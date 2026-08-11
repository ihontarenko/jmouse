package org.jmouse.access.el;

import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.access.el.node.SourceSpanNode;
import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * Reads positions and raw text back out of a {@link TokenCursor}.
 *
 * <p>A {@link Token} knows its offset but not the characters around it, which leaves two things the
 * language document insists on out of reach: a span that names a <em>column</em> rather than a
 * position in the whole file, and a condition preserved exactly as it was typed. Both need the
 * source, and this is where the parser goes to get it.</p>
 *
 * <p>A cursor is allowed not to have a source. Where that happens the offset stands in for the
 * column and a slice falls back to the tokens themselves — degraded, never broken.</p>
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
        TokenizableSource source = cursor.source();
        return source == null ? token.offset() : source.getColumnNumber(token.offset());
    }

    /**
     * Returns the source text spanning a run of tokens, exactly as it was written.
     *
     * <p>⚠️ Rendering the same run back from its parsed tree would give <em>a</em> spelling of it,
     * not the one in the file — different spacing, different parentheses, a reader who cannot find
     * the line. Anything shown back to an administrator has to come from here.</p>
     *
     * @param cursor the cursor the tokens were read from
     * @param first  the first token of the run
     * @param last   the last token of the run, inclusive
     * @return the verbatim text, trimmed of surrounding whitespace
     */
    public static String text(TokenCursor cursor, Token first, Token last) {
        TokenizableSource source = cursor.source();

        if (source == null) {
            return first.value();
        }

        int start = Math.max(0, first.offset());
        int end   = Math.min(source.length(), last.offset() + last.value().length());

        return start >= end ? first.value() : source.subSequence(start, end).toString().trim();
    }

    /**
     * Reads a token's value with any surrounding quotes removed.
     *
     * <p>A quoted identifier exists so a name can hold what the bare form cannot; the quotes are how
     * it was written, never part of what it says.</p>
     *
     * @param token the token to read
     * @return the token's value, unquoted
     */
    public static String literal(Token token) {
        return MimeParser.unquote(token.value());
    }

    /**
     * Reads a name that may contain hyphens — {@code storage-byte}, {@code parametric-search} — or a
     * quoted string.
     *
     * <p>⚠️ <strong>The lexer does not produce these as one token.</strong> {@code storage-byte}
     * arrives as {@code storage}, {@code -}, {@code byte}, exactly as {@code form:read} arrives as
     * three tokens and is joined by a run of the same kind. Quoting would also work — {@code 'own-rows'}
     * in a scopes block already does — but a capability key appears on every line of every bundle, and
     * a catalogue in quotes is a catalogue nobody enjoys proofreading, which is the whole reason it
     * moved out of a migration and into a document.
     *
     * <p>The run cannot swallow what follows: nothing that may come after a key — a description, a
     * number, {@code per}, a comma, the end of the line — begins with a hyphen.
     *
     * @param cursor the cursor, positioned on the first token of the name
     * @return the name, hyphens included and quotes removed
     */
    public static String hyphenatedName(TokenCursor cursor) {
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return literal(cursor.ensure(BasicToken.T_STRING));
        }

        StringBuilder name = new StringBuilder(cursor.ensure(AccessToken.namesAndKeywords()).value());

        while (cursor.isCurrent(BasicToken.T_MINUS) && cursor.isNext(AccessToken.namesAndKeywords())) {
            cursor.ensure(BasicToken.T_MINUS);
            name.append('-').append(cursor.ensure(AccessToken.namesAndKeywords()).value());
        }

        return name.toString();
    }
}
