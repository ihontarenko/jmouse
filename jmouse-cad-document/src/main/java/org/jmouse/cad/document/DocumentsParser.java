package org.jmouse.cad.document;

import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

/**
 * A {@code .jm} text: one document or more. The engine's statement reader walks it; its statements are documents, with
 * no braces around them — the text opens at its start and closes at its end.
 */
public class DocumentsParser extends StatementsParser {

    @Override
    protected void open(TokenCursor cursor) {
        cursor.consumeIf(BasicToken.T_SOL);
    }

    @Override
    protected boolean isClosed(TokenCursor cursor) {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    @Override
    protected void close(TokenCursor cursor) {
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        return (Expression) context.getParser(DocumentParser.class).parse(cursor, context);
    }
}
