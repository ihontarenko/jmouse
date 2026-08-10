package org.jmouse.el.language.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.parser.ParserContext;

public abstract class AbstractBranchParser<N extends ExpressionsNode, B extends ExpressionsNode, T extends Token.Type>
        extends AbstractBlockParser<N, T> {

    @Override
    protected final void parseBlock(TokenCursor cursor, N node, ParserContext context) {
        do {
            parseBranch(cursor, node, context);
        } while (hasNextBranch(cursor));
    }

    protected void parseBranch(TokenCursor cursor, N node, ParserContext context) {
        B branch = createBranch(cursor, node, context);
        parseBranchHeader(cursor, branch, context);
        parseBranchBody(cursor, branch, context);
        addBranch(node, branch);
    }

    protected void parseBranchBody(TokenCursor cursor, B branch, ParserContext context) {
        Node children = context.getParser(StatementsParser.class).parse(cursor, context);
        children.getChildren().stream().map(Expression.class::cast).forEach(branch::addExpression);
    }

    protected abstract B createBranch(TokenCursor cursor, N node, ParserContext context);

    protected abstract void parseBranchHeader(TokenCursor cursor, B branch, ParserContext context);

    protected abstract void addBranch(N node, B branch);

    protected abstract boolean hasNextBranch(TokenCursor cursor);

}
