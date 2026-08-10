package org.jmouse.el.language.parser;

import org.jmouse.core.context.ContextScope;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

public abstract class AbstractBlockParser<N extends ExpressionsNode, T extends Token.Type> extends AbstractParser {

    @Override
    public final void parse(TokenCursor cursor, Node parent, ParserContext context) {
        SpanNode span = span(cursor);

        cursor.ensure(token());

        N node = createNode(cursor, context);

        if (node.getSpan() == null) {
            node.setSpan(span);
        }

        try (ContextScope.ScopeToken ignored = context.getContextScope().open(scopeType())) {
            parseBlock(cursor, node, context);
            parent.add(node);
        }
    }

    protected abstract void parseBlock(TokenCursor cursor, N node, ParserContext context);

    protected abstract N createNode(TokenCursor cursor, ParserContext context);

    protected abstract T token();

    protected abstract boolean matches(TokenCursor cursor);

    protected abstract <S extends SpanNode> S span(TokenCursor cursor);

    protected Class<? extends Parser> scopeType() {
        return getClass();
    }

    @Override
    public final boolean supports(TokenCursor cursor) {
        return matches(cursor);
    }
}