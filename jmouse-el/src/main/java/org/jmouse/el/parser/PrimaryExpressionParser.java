package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.unary.PostfixUnaryOperation;
import org.jmouse.el.node.expression.unary.PrefixUnaryOperation;

import static org.jmouse.el.lexer.BasicToken.*;

public class PrimaryExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node left = null;

        // parse left expression
        if (cursor.matchesSequence(T_IDENTIFIER, BasicToken.T_OPEN_PAREN)) {
            left = context.getParser(FunctionParser.class).parse(cursor, context);
        } else if (cursor.matchesSequence(T_INT, T_DOUBLE_DOT)) {
            left = context.getParser(RangeParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL)) {
            left = context.getParser(LiteralParser.class).parse(cursor, context);
        }  else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT, T_IDENTIFIER, T_OPEN_PAREN)) {
            left = context.getParser(ScopedCallParser.class).parse(cursor, context);
        } else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT, T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            Token token = cursor.peek();
            cursor.next();
            left = new PrefixUnaryOperation((ExpressionNode) parse(cursor, context), context.getOperator(token.type()));
        } else if (cursor.matchesSequence(T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_BRACKET)) {
            left = context.getParser(ArrayParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_CURLY)) {
            left = context.getParser(MapParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_PAREN)) {
            cursor.ensure(T_OPEN_PAREN);
            left = context.getParser(OperatorParser.class).parse(cursor, context);
            cursor.ensure(T_CLOSE_PAREN);
        }

        if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            left = new PostfixUnaryOperation((ExpressionNode) left, context.getOperator(cursor.peek().type()));
            cursor.next();
        }

        if (left == null) {
            throw new ParseException("Unexpected token " + cursor.peek() + " at " + cursor.position());
        }

        parent.add(left);
    }

}
