package org.jmouse.el.parser;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;
import org.jmouse.el.node.expression.unary.PostfixUnaryOperation;
import org.jmouse.el.node.expression.unary.PrefixUnaryOperation;

import static org.jmouse.el.lexer.BasicToken.*;

public class PrimaryExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node left = null;

        // parse left expression
        if (CursorMatcher.function().matches(cursor)) {
            left = context.getParser(FunctionParser.class).parse(cursor, context);
        } if (CursorMatcher.lambda().matches(cursor)) {
            left = context.getParser(LambdaParser.class).parse(cursor, context);
        } else if (CursorMatcher.literal().matches(cursor)) {
            left = context.getParser(LiteralParser.class).parse(cursor, context);
        }  else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT, T_IDENTIFIER, T_OPEN_PAREN)) {
            left = context.getParser(ScopedCallParser.class).parse(cursor, context);
        } else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT, T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_NEGATE)) {
            cursor.next();
            left = new NegateUnaryOperation((Expression) parse(cursor, context));
        } else if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            Token token = cursor.peek();
            cursor.next();
            left = new PrefixUnaryOperation((Expression) parse(cursor, context), context.getOperator(token.type()));
        } else if (cursor.matchesSequence(T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_BRACKET)) {
            left = context.getParser(ArrayParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_CURLY)) {
            left = context.getParser(MapParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_OPEN_PAREN)) {
            cursor.ensure(T_OPEN_PAREN);
            left = context.getParser(ExpressionParser.class).parse(cursor, context);
            cursor.ensure(T_CLOSE_PAREN);
        }

        context.getParser(cursor);

        if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            left = new PostfixUnaryOperation((Expression) left, context.getOperator(cursor.peek().type()));
            cursor.next();
        }

        if (left == null) {
            throw new ParseException("Unexpected token " + cursor.peek() + " at " + cursor.position());
        }

        parent.add(left);
    }

}
