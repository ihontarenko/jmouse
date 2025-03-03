package org.jmouse.template.parser;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.*;
import org.jmouse.template.node.arithmetic.BinaryOperation;
import org.jmouse.template.node.arithmetic.PostfixUnaryOperation;
import org.jmouse.template.node.arithmetic.PrefixUnaryOperation;

import static org.jmouse.template.lexer.BasicToken.*;

/**
 * ArithmeticExpressionParser parses arithmetic expressions using recursive descent.
 * <p>
 * Supported features:
 * <ul>
 *   <li>Binary operators: +, -, *, /, ^</li>
 *   <li>Unary operators: prefix ++, --</li>
 *   <li>Parenthesized expressions</li>
 *   <li>Implicit multiplication: e.g., "3 (2+6)" is interpreted as "3 * (2+6)"</li>
 * </ul>
 * Operator precedence is handled as follows (from highest to lowest):
 * <ol>
 *   <li>Parentheses and primary expressions</li>
 *   <li>Prefix operators (including ++ and --)</li>
 *   <li>Postfix operators (++ and --)</li>
 *   <li>Exponential operator (^)</li>
 *   <li>Multiplicative operators (* and /, including implicit multiplication)</li>
 *   <li>Additive operators (+ and -)</li>
 * </ol>
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ArithmeticParser implements Parser {

    /**
     * Parses an arithmetic expression and adds it as a child of the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to attach the parsed expression
     * @param context the parsing context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor));
    }

    /**
     * Parses an arithmetic expression.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private Expression parseExpression(TokenCursor cursor) {
        return parseAdditiveExpression(cursor);
    }

    /**
     * Parses an additive expression, handling addition and subtraction.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private Expression parseAdditiveExpression(TokenCursor cursor) {
        Expression leftExpression = parseMultiplicativeExpression(cursor);

        while (cursor.hasNext() && cursor.isCurrent(T_PLUS, T_MINUS)) {
            Token.Type operator        = cursor.next().type();
            Expression rightExpression = parseMultiplicativeExpression(cursor);

            leftExpression = new BinaryOperation(leftExpression, operator, rightExpression);
        }

        return leftExpression;
    }

    /**
     * Parses a multiplicative expression, handling multiplication and division.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private Expression parseMultiplicativeExpression(TokenCursor cursor) {
        Expression leftExpression = parseExponentialExpression(cursor);

        while (cursor.hasNext() && cursor.isCurrent(T_MULTIPLY, T_DIVIDE)) {
            Token.Type operator        = cursor.next().type();
            Expression rightExpression = parseExponentialExpression(cursor);
            leftExpression = new BinaryOperation(leftExpression, operator, rightExpression);
        }

        // Implicit multiplication: if next token is LEFT_PAREN, treat it as multiplication.
        while (cursor.hasNext() && cursor.isCurrent(T_OPEN_PAREN)) {
            // Implicit '*' operator
            cursor.next();
            leftExpression = new BinaryOperation(leftExpression, T_MULTIPLY, parseExpression(cursor));
        }

        return leftExpression;
    }

    /**
     * Parses an exponential expression, handling the '^' operator.
     *
     * @return the parsed exponential expression AST
     */
    private Expression parseExponentialExpression(TokenCursor cursor) {
        Expression leftExpression = parseUnaryExpression(cursor);

        while (cursor.hasNext() && cursor.isCurrent(T_CARET)) {
            cursor.next(); // consume '^'
            leftExpression = new BinaryOperation(leftExpression, T_CARET, parseUnaryExpression(cursor));
        }

        return leftExpression;
    }

    /**
     * Parses a unary expression, handling prefix operators such as {@code +}, {@code -}, {@code ++}, and {@code --}.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private Expression parseUnaryExpression(TokenCursor cursor) {
        Expression expression;

        // Handle prefix increment/decrement (++i, --i)
        if (cursor.hasNext() && cursor.isCurrent(T_INCREMENT, T_DECREMENT)) {
            Token.Type operator = cursor.next().type();
            Expression operand  = parseUnaryExpression(cursor);
            return new PrefixUnaryOperation(operand, operator);
        }

        // Parse primary expression
        expression = parsePrimaryExpression(cursor);

        // Handle postfix increment/decrement (i++, i--)
        if (cursor.hasNext() && cursor.isCurrent(T_INCREMENT, T_DECREMENT)) {
            Token.Type operator = cursor.next().type();
            return new PostfixUnaryOperation(expression, operator);
        }

        return expression;
    }

    /**
     * Parses a primary expression, such as a number or a parenthesized expression.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private Expression parsePrimaryExpression(TokenCursor cursor) {
        Expression expression;

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            cursor.next(); // Consume '('
            expression = parseExpression(cursor);
            cursor.expect(BasicToken.T_CLOSE_PAREN); // Ensure closing ')'
        } else {
            expression = new NumberExpression(Double.parseDouble(cursor.peek().value()));
            cursor.next();
        }

        return expression;
    }
}
