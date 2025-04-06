package org.jmouse.el.core.parser;

import org.jmouse.el.core.extension.Operator;
import org.jmouse.el.core.extension.operator.TestOperator;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.BinaryOperation;
import org.jmouse.el.core.node.expression.TestNode;

import static org.jmouse.el.core.lexer.BasicToken.*;

/**
 * Parses arithmetic and logical expressions with operator precedence.
 * <p>
 * Supports nested expressions, implicit multiplication (e.g. 2(3 + 4) → 2 * (3 + 4)), and test operators.
 * </p>
 */
public class OperatorParser implements Parser {

    /**
     * Parses an expression and attaches it to the parent node.
     *
     * @param cursor  the token stream cursor
     * @param parent  the parent node to add the parsed expression
     * @param context the parser context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor, context, 0));
    }

    /**
     * Parses an expression considering operator precedence.
     *
     * @param cursor     the token cursor
     * @param context    the parser context
     * @param precedence the current precedence level
     * @return the parsed expression node
     */
    private ExpressionNode parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        ExpressionNode left = parsePrimaryExpression(cursor, context);

        while (cursor.hasNext()) {
            Token    token    = cursor.peek();
            Operator operator = context.getOperator(token.type());

            if (operator == null || precedence > operator.getPrecedence()) {
                break;
            }

            cursor.next();

            if (operator == TestOperator.IS) {
                TestNode test = (TestNode) context.getParser(TestParser.class).parse(cursor, context);
                test.setLeft(left);
                left = test;
            } else {
                ExpressionNode right = parseExpression(cursor, context, operator.getPrecedence() + 1);
                left = new BinaryOperation(left, operator, right);
            }
        }
        return left;
    }

    /**
     * Parses a primary expression.
     *
     * @param cursor  the token cursor
     * @param context the parser context
     * @return the parsed primary expression node
     */
    private ExpressionNode parsePrimaryExpression(TokenCursor cursor, ParserContext context) {
        PrimaryExpressionParser parser = (PrimaryExpressionParser) context.getParser(PrimaryExpressionParser.class);
        ExpressionNode          left;

        // Parenthesized expression
        if (cursor.isCurrent(T_OPEN_PAREN) && !cursor.isPrevious(T_IDENTIFIER)) {
            cursor.ensure(T_OPEN_PAREN);
            left = parseExpression(cursor, context, 0);
            cursor.ensure(T_CLOSE_PAREN);
        } else {
            left = (ExpressionNode) parser.parse(cursor, context);
        }

        // Implicit multiplication: e.g., 2(3 + 4) becomes 2 * (3 + 4)
        if (cursor.isCurrent(T_OPEN_PAREN)) {
            Operator multiply = context.getOperator(T_MULTIPLY);
            left = new BinaryOperation(left, multiply, parsePrimaryExpression(cursor, context));
        }

        return left;
    }
}
