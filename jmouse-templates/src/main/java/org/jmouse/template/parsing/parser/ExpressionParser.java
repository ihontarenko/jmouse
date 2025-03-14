package org.jmouse.template.parsing.parser;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.FilterExpression;
import org.jmouse.template.node.expression.unary.PostfixUnaryOperation;
import org.jmouse.template.node.expression.unary.PrefixUnaryOperation;
import org.jmouse.template.parsing.ParseException;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;

public class ExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node left = null;

        // parse left expression
        if (cursor.matchesSequence(T_IDENTIFIER, T_OPEN_PAREN)) {
            left = context.getParser(FunctionParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL)) {
            left = context.getParser(LiteralParser.class).parse(cursor, context);
        } else if (cursor.matchesSequence(T_IDENTIFIER, T_DOT, T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        } else if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            Token token = cursor.peek();
            cursor.next();
            left = new PrefixUnaryOperation((ExpressionNode) parse(cursor, context), context.getOperator(token.type()));
        } else if (cursor.matchesSequence(T_IDENTIFIER)) {
            left = context.getParser(PropertyParser.class).parse(cursor, context);
        }

        if (cursor.isCurrent(T_DECREMENT, T_INCREMENT)) {
            left = new PostfixUnaryOperation((ExpressionNode) left, context.getOperator(cursor.peek().type()));
            cursor.next();
        }

        // parse right expression if present
        while (cursor.matchesSequence(T_VERTICAL_SLASH, T_IDENTIFIER)) {
            cursor.expect(T_IDENTIFIER);
            String name = cursor.peek().value();
            left = new FilterExpression(context.getFilter(name), left);
            cursor.next();
        }

        if (left == null) {
            throw new ParseException("Unrecognized expression at " + cursor.position());
        }

        parent.add(left);
    }

}
