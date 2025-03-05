package org.jmouse.template.parser.arithmetic;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.arithmetic.PostfixUnaryOperation;
import org.jmouse.template.node.expression.arithmetic.PrefixUnaryOperation;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import static org.jmouse.template.lexer.BasicToken.T_DECREMENT;
import static org.jmouse.template.lexer.BasicToken.T_INCREMENT;

/**
 * UnaryExpressionParser parses unary expressions.
 * <p>
 * Grammar:
 * </p>
 *
 * <pre>{@code
 *   UnaryExpression ::= [ ("++" | "--") ] PrimaryExpression { ("++" | "--") }
 * }</pre>
 *
 * <p>
 * This parser handles both prefix and postfix unary operators.
 * </p>
 */
public class UnaryExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Empty implementation.
    }

    public ExpressionNode parseExpression(TokenCursor cursor, ParserContext context) {
        ExpressionNode expression;

        // Handle prefix increment/decrement (++i, --i)
        if (cursor.hasNext() && cursor.isCurrent(T_INCREMENT, T_DECREMENT)) {
            Token.Type     operator = cursor.next().type();
            ExpressionNode operand  = parseExpression(cursor, context);
            return new PrefixUnaryOperation(operand, operator);
        }

        // Parse primary expression
        expression = (ExpressionNode) context.getParser("operand").parse(cursor, context);

        // Handle postfix increment/decrement (i++, i--)
        if (cursor.hasNext() && cursor.isCurrent(T_INCREMENT, T_DECREMENT)) {
            Token.Type operator = cursor.next().type();
            return new PostfixUnaryOperation(expression, operator);
        }

        return expression;
    }

    @Override
    public String getName() {
        return "unary";
    }

}
