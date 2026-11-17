package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.json.JsonSyntaxException;

/**
 * 🧾 What every JSON parser does around a token: line breaks are space between tokens, and a token that is not the one
 * expected is refused with what was expected.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonParsing {

    private JsonParsing() {
    }

    /** Passes over line breaks: in JSON they are space. */
    public static void skipLines(TokenCursor cursor) {
        while (cursor.isCurrent(BasicToken.T_NEW_LINE)) {
            cursor.next();
        }
    }

    /** The token of {@code type}, taken; refused, with {@code expected} said, where another stands. */
    public static Token expect(TokenCursor cursor, BasicToken type, String expected) {
        skipLines(cursor);
        if (!cursor.isCurrent(type)) {
            throw new JsonSyntaxException(expected, cursor.peek());
        }
        return cursor.next();
    }

    /** Whether the next token, past line breaks, is of {@code type}. */
    public static boolean isNext(TokenCursor cursor, BasicToken type) {
        skipLines(cursor);
        return cursor.isCurrent(type);
    }

    /** Takes a comma where the next token is one: whether there was. */
    public static boolean consumeComma(TokenCursor cursor) {
        if (isNext(cursor, BasicToken.T_COMMA)) {
            cursor.next();
            return true;
        }
        return false;
    }
}
