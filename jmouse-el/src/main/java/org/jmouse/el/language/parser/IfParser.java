package org.jmouse.el.language.parser;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.language.node.IfBranchNode;
import org.jmouse.el.language.node.IfNode;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.ParserContext;

import static org.jmouse.el.language.lexer.LanguageToken.*;

public class IfParser extends AbstractBranchParser<IfNode, IfBranchNode, LanguageToken> {

    @Override
    protected IfNode createNode(TokenCursor cursor, ParserContext context) {
        return new IfNode();
    }

    @Override
    protected LanguageToken token() {
        return T_IF;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return cursor.isCurrent(T_IF);
    }

    @Override
    protected IfBranchNode createBranch(TokenCursor cursor, IfNode node, ParserContext context) {
        return new IfBranchNode();
    }

    @Override
    protected void parseBranchHeader(TokenCursor cursor, IfBranchNode branch, ParserContext context) {
        if (cursor.isPrevious(T_IF, T_ELSE_IF)) {
            branch.setCondition(parseCondition(cursor, context));
        }
    }

    @Override
    protected boolean hasNextBranch(TokenCursor cursor) {
        return cursor.consumeIf(T_ELSE_IF, T_ELSE);
    }

    @Override
    protected void addBranch(IfNode node, IfBranchNode branch) {
        node.addBranch(branch);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        Token token = cursor.current();
        return (S) SpanNode.of(token.lineNumber(), token.offset());
    }

    private Expression parseCondition(TokenCursor cursor, ParserContext context) {
        return (Expression) context.getParser(OperatorParser.class).parse(cursor, context);
    }

}