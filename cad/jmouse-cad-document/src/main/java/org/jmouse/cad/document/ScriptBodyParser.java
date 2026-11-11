package org.jmouse.cad.document;

import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;

/**
 * A jMS body read on its own — the text a {@code behaviour} block held between its braces —: the same statements as in
 * the block, ended by the end of the text rather than by a brace.
 */
final class ScriptBodyParser extends StatementsParser {

    /** A text opens with where it starts, as a body opens with its brace. */
    @Override
    protected void open(TokenCursor cursor) {
        if (cursor.isCurrent(BasicToken.T_SOL)) {
            cursor.next();
        }
    }

    @Override
    protected boolean isClosed(TokenCursor cursor) {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    @Override
    protected void close(TokenCursor cursor) {
    }
}
