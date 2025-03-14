package org.jmouse.template.parsing.parser;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.BinaryOperation;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;

/**
 * 🔢 OperatorParser is responsible for parsing mathematical and logical expressions.
 * <p>
 * Supports:
 * <ul>
 *     <li>Logical: {@code a > b or (b < c and x == z) or w is true }</li>
 *     <li>Arithmetic: {@code 10 + (2 + 2) * 2 (22 / 7) }</li>
 * </ul>
 * <p>
 * Handles:
 * <ul>
 *     <li>Precedence-based parsing</li>
 *     <li>Implicit multiplication (e.g., {@code 2(3 + 4) → 2 * (3 + 4)})</li>
 *     <li>Nested expressions with parentheses</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OperatorParser implements Parser {

    /**
     * Parses an operation tag and adds it as a child of the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to attach the parsed tag
     * @param context the parsing context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor, context, 0));
    }

    /**
     * Parses an arithmetic or logical expression, considering operator precedence.
     *
     * @param cursor     the token cursor
     * @param context    the parsing context
     * @param precedence the current operator precedence level
     * @return an expression node representing the parsed expression
     */
    private ExpressionNode parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        ExpressionNode left = parsePrimaryExpression(cursor, context);

        while (cursor.hasNext()) {
            Token    token    = cursor.peek();
            Operator operator = context.getOperator(token.type());

            // Stop if operator precedence is lower than the current precedence
            if (operator == null || precedence > operator.getPrecedence()) {
                break;
            }

            cursor.next(); // Consume operator

            // Parse the right-hand side with adjusted precedence
            ExpressionNode right = parseExpression(cursor, context, operator.getPrecedence() + 1);

            left = new BinaryOperation(left, operator, right);
        }

        return left;
    }

    /**
     * Parses a primary expression, which could be:
     * <ul>
     *     <li>A nested expression within parentheses</li>
     *     <li>A function call or a variable</li>
     *     <li>An implicit multiplication (e.g., {@code 2(3 + 4)})</li>
     * </ul>
     *
     * @param cursor  the token cursor
     * @param context the parsing context
     * @return the parsed primary expression node
     */
    private ExpressionNode parsePrimaryExpression(TokenCursor cursor, ParserContext context) {
        ExpressionParser parser = (ExpressionParser) context.getParser(ExpressionParser.class);
        ExpressionNode   left;

        // Handle nested expressions inside parentheses
        if (cursor.isCurrent(T_OPEN_PAREN) && !cursor.isPrevious(T_IDENTIFIER)) {
            cursor.ensure(T_OPEN_PAREN);
            left = parseExpression(cursor, context, 0);
            cursor.ensure(T_CLOSE_PAREN);
        } else {
            // Parse any standard expression (number, variable, function, etc.)
            left = (ExpressionNode) parser.parse(cursor, context);
        }

        // Handle implicit multiplication: e.g., `2 (3 + 4)` -> `2 * (3 + 4)`
        if (cursor.isCurrent(T_OPEN_PAREN)) {
            Operator multiply = context.getOperator(T_MULTIPLY);
            left = new BinaryOperation(left, multiply, parsePrimaryExpression(cursor, context));
        }

        return left;
    }
}
