package org.jmouse.el.grammar;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Trivia;

import java.util.Set;

/**
 * What a line-oriented language says of its lines, as lookaheads over tokens anchored at the cursor: where a line ends,
 * what ends a word, what a number is. A grammar's values end where these say; nothing scans ahead for a token it hopes
 * to find.
 */
public final class Lines {

    /** What ends a word written of several tokens even without a space: {@code units A, B} is two names. */
    public static final Set<String> WORD_SEPARATORS = Set.of(",", ";", "=", "(", ")", "[", "]", "{", "}", ":");

    /** What opens a comment, as the lexer reads it: written before a comment's text. */
    public static final String COMMENT_MARK = BasicToken.T_HASH.getTokenTemplates()[0];

    /** A comment written above a statement, its text without the mark that opens it. */
    public static Trivia comment(String text) {
        return Trivia.comment(COMMENT_MARK + " " + text);
    }

    /** The kinds of token a number is read as. */
    private static final Set<BasicToken> NUMBERS = Set.of(BasicToken.T_INT, BasicToken.T_NUMERIC, BasicToken.T_SHORT,
            BasicToken.T_BYTE, BasicToken.T_LONG, BasicToken.T_FLOAT, BasicToken.T_DOUBLE);

    /** A value read through the engine's {@link BoundedTokenCursor} ends where its line does. */
    public static final BoundedTokenCursor.Boundary LINE = (token, next, depth) -> ends(token);

    private Lines() {
    }

    /** Where a line ends: a line break, a {@code ;}, a brace, a comment, the end of the text. */
    public static Matcher<TokenCursor> lineEnd() {
        return cursor -> !cursor.hasNext() || ends(cursor.current());
    }

    /** Whether the line ends at the token {@code offset} ahead of the cursor. */
    public static boolean endsAt(TokenCursor cursor, int offset) {
        return offset >= cursor.remaining() || ends(cursor.lookAt(offset));
    }

    private static boolean ends(Token token) {
        return token == null || token.type() == BasicToken.T_NEW_LINE || token.type() == BasicToken.T_SEMICOLON
                || token.type() == BasicToken.T_OPEN_CURLY || token.type() == BasicToken.T_CLOSE_CURLY
                || token.type() == BasicToken.T_HASH || token.type() == BasicToken.T_EOL;
    }

    /** Whether a token is a number. */
    public static boolean number(Token token) {
        return token != null && token.type() instanceof BasicToken type && NUMBERS.contains(type);
    }

    /** Whether a number stands at the cursor, a minus written against it and all: {@code -20}. */
    public static boolean signedNumber(TokenCursor cursor) {
        Token current = cursor.current();
        return number(current) || current.type() == BasicToken.T_MINUS && number(cursor.lookAt(1))
                && SourceReading.touches(cursor, current, cursor.lookAt(1));
    }

    /** The word the cursor stands on, the cursor left where it was. */
    public static String peekWord(TokenCursor cursor) {
        TokenCursor.Savepoint savepoint = cursor.savepoint();
        String word = SourceReading.touchingWord(cursor, WORD_SEPARATORS);
        cursor.restore(savepoint);
        return word;
    }

    /** Steps over a name, an index written against it and all: {@code R1}, {@code q[0..7]}. */
    public static void skipName(TokenCursor cursor) {
        SourceReading.touchingWord(cursor, WORD_SEPARATORS);
        Token last = cursor.lookAt(-1);
        if (cursor.hasNext() && cursor.isCurrent(BasicToken.T_OPEN_BRACKET) && SourceReading.touches(cursor, last, cursor.current())) {
            while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_CLOSE_BRACKET) && !lineEnd().matches(cursor)) {
                cursor.next();
            }
            if (cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                cursor.next();
            }
        }
    }

    /** One word as its author wrote it — {@code °C}, not what the lexer read it as —, the cursor left after it. */
    public static String word(TokenCursor cursor) {
        Token first = cursor.current();
        SourceReading.touchingWord(cursor, WORD_SEPARATORS);
        return AuthoredText.of(cursor).written(first, cursor.lookAt(-1));
    }
}
