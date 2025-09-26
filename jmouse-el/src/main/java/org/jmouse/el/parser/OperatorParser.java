package org.jmouse.el.parser;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.FilterOperator;
import org.jmouse.el.extension.operator.NullCoalesceOperator;
import org.jmouse.el.extension.operator.TestOperator;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.node.expression.NullSafeFallbackNode;
import org.jmouse.el.node.expression.TestNode;

import static org.jmouse.el.lexer.BasicToken.*;

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
    private Expression parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        Expression left = parsePrimaryExpression(cursor, context);

        while (cursor.hasNext()) {
            Token    token    = cursor.peek();
            Operator operator = context.getOperator(token.type());

            if (operator == null || precedence > operator.getPrecedence()) {
                break;
            }

            cursor.next();

            switch (operator) {
                case TestOperator.IS -> {
                    TestNode test = (TestNode) context.getParser(TestParser.class).parse(cursor, context);
                    test.setLeft(left);
                    left = test;
                }
                case NullCoalesceOperator.NULL_COALESCE -> {
                    NullSafeFallbackNode node = new NullSafeFallbackNode();
                    node.setNullable(left);
                    node.setOtherwise((Expression) parse(cursor, context));
                    left = node;
                }
                case FilterOperator.FILTER -> {
                    while (cursor.hasNext() && cursor.isCurrent(T_IDENTIFIER)) {
                        if (context.getParser(FilterParser.class).parse(cursor, context) instanceof FilterNode filter) {
                            filter.setLeft(left);
                            left = filter;
                        }
                    }
                }
                default -> {
                    Expression right = parseExpression(cursor, context, operator.getPrecedence() + 1);
                    left = new BinaryOperation(left, operator, right);
                }
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
    private Expression parsePrimaryExpression(TokenCursor cursor, ParserContext context) {
        PrimaryExpressionParser parser = (PrimaryExpressionParser) context.getParser(PrimaryExpressionParser.class);
        Expression              left   = (Expression) parser.parse(cursor, context);

        // Implicit multiplication: e.g., 2(3 + 4) becomes 2 * (3 + 4)
        if (cursor.isCurrent(T_OPEN_PAREN)) {
            Operator multiply = context.getOperator(T_MULTIPLY);
            left = new BinaryOperation(left, multiply, parsePrimaryExpression(cursor, context));
        }

        return left;
    }
}
