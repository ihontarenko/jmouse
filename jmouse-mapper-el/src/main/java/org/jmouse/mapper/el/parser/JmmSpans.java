package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.SpanNode;

/**
 * Where a node came from, taken the one way. 📍
 *
 * <p>Written once because a span taken slightly differently in one parser is a line number that is
 * wrong in one construction and right in every other — the kind of difference nobody notices until
 * they are chasing the wrong line.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmSpans {

    private JmmSpans() {
    }

    /**
     * The span of whatever the cursor is on.
     *
     * @param cursor the cursor, positioned on the construction's first token
     * @return the span
     */
    public static SpanNode at(TokenCursor cursor) {
        return at(cursor, cursor.current());
    }

    /**
     * The span of one token, read through a cursor that knows its source.
     *
     * @param cursor the cursor the token came from
     * @param token  the token to point at
     * @return the span
     */
    public static SpanNode at(TokenCursor cursor, Token token) {
        return SpanNode.of(token.lineNumber(), SourceReading.column(cursor, token));
    }
}
