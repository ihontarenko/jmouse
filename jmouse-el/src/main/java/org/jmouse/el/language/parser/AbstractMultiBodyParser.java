package org.jmouse.el.language.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.parser.ParserContext;

public abstract class AbstractMultiBodyParser<N extends ExpressionsNode, T extends Token.Type> extends AbstractBlockParser<N, T> {

    @Override
    protected final void parseBlock(TokenCursor cursor, N node, ParserContext context) {
        do {
            parseBody(cursor, node, context);
        } while (hasNextBody(cursor));
    }

    protected abstract void parseBody(TokenCursor cursor, N node, ParserContext context);

    protected abstract boolean hasNextBody(TokenCursor cursor);
}