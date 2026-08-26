package org.jmouse.el.lexer.support;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

/**
 * Reads positions and raw text back out of a {@link TokenCursor}. 📖
 *
 * <p>A {@link Token} knows its offset but not the characters around it, which leaves two things any
 * language with an editable source insists on out of reach: a span that names a <em>column</em> rather
 * than a position in the whole file, and a construct preserved exactly as it was typed. Both need the
 * source, and this is where a parser goes to get it.</p>
 *
 * <p>A cursor is allowed not to have a source. Where that happens the offset stands in for the column
 * and a slice falls back to the tokens themselves — degraded, never broken.</p>
 *
 * <h2>⚠️ Line and column, never a node</h2>
 *
 * <p>This deliberately hands back numbers rather than a {@link org.jmouse.el.node.expression.SpanNode}.
 * A language usually wants its own span node — one that converts into whatever its own model records a
 * position as — and a shared helper that picked the node type would force every language to use the
 * first one written. Build the node where the node is known.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceReading {

    private SourceReading() {
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
     * Returns the 1-based column of the token the cursor currently sits on.
     *
     * @param cursor the cursor to read from
     * @return the current token's column
     */
    public static int column(TokenCursor cursor) {
        return column(cursor, cursor.current());
    }

    /**
     * Returns the source text spanning a run of tokens, exactly as it was written.
     *
     * <p>⚠️ Rendering the same run back from its parsed tree would give <em>a</em> spelling of it, not
     * the one in the file — different spacing, different parentheses, a reader who cannot find the
     * line. Anything shown back to a person has to come from here.</p>
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
     * <p>A quoted identifier exists so a name can hold what the bare form cannot; the quotes are how it
     * was written, never part of what it says.</p>
     *
     * @param token the token to read
     * @return the token's value, unquoted
     */
    public static String literal(Token token) {
        String value = token.value();

        if (value != null && value.length() > 1) {
            char first = value.charAt(0);
            char last  = value.charAt(value.length() - 1);

            if (first == last && (first == '"' || first == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    /**
     * Reads a name joined by a separator — {@code storage-byte}, {@code shop.api.OrderRequest} — or a
     * quoted string standing in for one.
     *
     * <p>⚠️ <strong>The lexer does not produce these as one token.</strong> {@code storage-byte} arrives
     * as {@code storage}, {@code -}, {@code byte}. Joining them is a run of the same kind, and it cannot
     * swallow what follows as long as nothing legal after the name begins with the separator.</p>
     *
     * <p>⚠️ <strong>{@code nameTokens} is a parameter, and that is the point.</strong> Every language on
     * this lexer has its own keywords, and every one of those keywords is a plausible name — a helper
     * hard-wired to one language's set is not shared, it is borrowed. Pass the set that includes
     * {@link BasicToken#T_IDENTIFIER} and this language's own words.</p>
     *
     * @param cursor     the cursor, positioned on the first token of the name
     * @param separator  the token joining the parts — a dot, a hyphen
     * @param nameTokens what may stand as one part of the name
     * @return the name, separators included and quotes removed
     */
    public static String joinedName(TokenCursor cursor, Token.Type separator, Token.Type... nameTokens) {
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return literal(cursor.ensure(BasicToken.T_STRING));
        }

        StringBuilder name      = new StringBuilder(cursor.ensure(nameTokens).value());
        String        separated = separator.getTokenTemplates()[0];

        while (cursor.isCurrent(separator) && cursor.isNext(nameTokens)) {
            cursor.ensure(separator);
            name.append(separated).append(cursor.ensure(nameTokens).value());
        }

        return name.toString();
    }

    /**
     * Reads a dotted name — {@code shop.api.OrderRequest}, {@code entry.listByPurpose}.
     *
     * @param cursor     the cursor, positioned on the first token of the name
     * @param nameTokens what may stand as one part of the name
     * @return the name, dots included and quotes removed
     */
    public static String dottedName(TokenCursor cursor, Token.Type... nameTokens) {
        return joinedName(cursor, BasicToken.T_DOT, nameTokens);
    }

    /**
     * Reads a hyphenated name — {@code storage-byte}, {@code parametric-search}.
     *
     * @param cursor     the cursor, positioned on the first token of the name
     * @param nameTokens what may stand as one part of the name
     * @return the name, hyphens included and quotes removed
     */
    public static String hyphenatedName(TokenCursor cursor, Token.Type... nameTokens) {
        return joinedName(cursor, BasicToken.T_MINUS, nameTokens);
    }
}
