package org.jmouse.template.parser.arithmetic;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.*;
import org.jmouse.template.node.expression.arithmetic.BinaryOperation;
import org.jmouse.template.node.expression.arithmetic.PostfixUnaryOperation;
import org.jmouse.template.node.expression.arithmetic.PrefixUnaryOperation;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import static org.jmouse.template.lexer.BasicToken.*;

/**
 * ArithmeticExpressionParser parses arithmetic expressions using recursive descent.
 *
 * <p>
 * Grammar (comprehensive):
 * <pre>{@code
 *   ArithmeticExpression ::= AdditiveExpression
 *
 *   AdditiveExpression   ::= MultiplicativeExpression { ("+" | "-") MultiplicativeExpression }
 *
 *   MultiplicativeExpression ::= ExponentialExpression { ("*" | "/" | ImplicitMultiplication) ExponentialExpression }
 *
 *   ExponentialExpression  ::= UnaryExpression { "^" UnaryExpression }
 *
 *   UnaryExpression      ::= [ ("++" | "--" | "+" | "-") ] PrimaryExpression { ("++" | "--") }
 *
 *   PrimaryExpression    ::= "(" ArithmeticExpression ")" | NUMBER | IDENTIFIER
 * }</pre>
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ArithmeticParser implements Parser {

    public static final String NAME = "arithmetic";

    /**
     * Parses an arithmetic expression and adds it as a child of the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to attach the parsed expression
     * @param context the parsing context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor, context));
    }

    /**
     * Parses an arithmetic expression.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private ExpressionNode parseExpression(TokenCursor cursor, ParserContext context) {
        return parseAdditiveExpression(cursor, context);
    }

    /**
     * Parses an additive expression, handling addition and subtraction.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private ExpressionNode parseAdditiveExpression(TokenCursor cursor, ParserContext context) {
        ExpressionNode leftExpression = parseMultiplicativeExpression(cursor, context);

        while (cursor.hasNext() && cursor.isCurrent(T_PLUS, T_MINUS)) {
            Token.Type     operator        = cursor.next().type();
            ExpressionNode rightExpression = parseMultiplicativeExpression(cursor, context);

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
    private ExpressionNode parseMultiplicativeExpression(TokenCursor cursor, ParserContext context) {
        ExpressionNode leftExpression = parseExponentialExpression(cursor, context);

        while (cursor.hasNext() && cursor.isCurrent(T_MULTIPLY, T_DIVIDE)) {
            Token.Type     operator        = cursor.next().type();
            ExpressionNode rightExpression = parseExponentialExpression(cursor, context);
            leftExpression = new BinaryOperation(leftExpression, operator, rightExpression);
        }

        // Implicit multiplication: if next token is LEFT_PAREN, treat it as multiplication.
        while (cursor.hasNext() && cursor.isCurrent(T_OPEN_PAREN)) {
            // Implicit '*' operator
            cursor.next();
            leftExpression = new BinaryOperation(leftExpression, T_MULTIPLY, parseExpression(cursor, context));
        }

        return leftExpression;
    }

    /**
     * Parses an exponential expression, handling the '^' operator.
     *
     * @return the parsed exponential expression AST
     */
    private ExpressionNode parseExponentialExpression(TokenCursor cursor, ParserContext context) {
        UnaryExpressionParser parser         = (UnaryExpressionParser) context.getParser("unary");
        ExpressionNode        leftExpression = parser.parseExpression(cursor, context);

        while (cursor.hasNext() && cursor.isCurrent(T_CARET)) {
            cursor.next(); // consume '^'
            leftExpression = new BinaryOperation(leftExpression, T_CARET, parser.parseExpression(cursor, context));
        }

        return leftExpression;
    }

    /**
     * Parses a unary expression, handling prefix operators such as {@code ++}, and {@code --}.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private ExpressionNode parseUnaryExpression(TokenCursor cursor, ParserContext context) {
        ExpressionNode expression;

        // Handle prefix increment/decrement (++i, --i)
        if (cursor.hasNext() && cursor.isCurrent(T_INCREMENT, T_DECREMENT)) {
            Token.Type     operator = cursor.next().type();
            ExpressionNode operand  = parseUnaryExpression(cursor, context);
            return new PrefixUnaryOperation(operand, operator);
        }

        // Parse primary expression
        expression = parsePrimaryExpression(cursor, context);

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
    private ExpressionNode parsePrimaryExpression(TokenCursor cursor, ParserContext context) {
        ExpressionNode expression;

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            cursor.next(); // Consume '('
            expression = parseExpression(cursor, context);
            cursor.expect(BasicToken.T_CLOSE_PAREN); // Ensure closing ')'
        } else {
            // expression = context.getParser(AnyExpressionParser.class).parse(); // infinite recursion
            expression = new NumberExpressionNode(Double.parseDouble(cursor.peek().value()));
            cursor.next();
        }

        return expression;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
