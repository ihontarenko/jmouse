package org.jmouse.el.language.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.parser.ParserContext;

public abstract class AbstractBodyParser<N extends ExpressionsNode, T extends Token.Type> extends AbstractBlockParser<N, T> {

    @Override
    protected void parseBlock(TokenCursor cursor, N node, ParserContext context) {
        parseBody(cursor, node, context);
    }

    protected void parseBody(TokenCursor cursor, N node, ParserContext context) {
        Node children = parseStatements(cursor, context);
        children.getChildren().stream().map(Expression.class::cast).forEach(node::addExpression);
    }

    protected Node parseStatements(TokenCursor cursor, ParserContext context) {
        return context.getParser(StatementsParser.class).parse(cursor, context);
    }
}