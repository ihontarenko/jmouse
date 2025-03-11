package org.jmouse.template.parser.global;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.BinaryOperation;
import org.jmouse.template.parser.ParseException;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

/**
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OperatorParser implements Parser {

    /**
     * Parses an arithmetic expression and adds it as a child of the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to attach the parsed expression
     * @param context the parsing context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(parseExpression(cursor, context, 0));
    }

    /**
     * Parses an arithmetic expression.
     *
     * @param cursor the token cursor
     * @return the parsed expression node
     */
    private ExpressionNode parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        if (context.getParser(RootParser.class) instanceof RootParser rootParser) {
            ExpressionNode left   = (ExpressionNode) rootParser.parse(cursor, context);

            while (cursor.hasNext()) {
                Operator operator = context.getOperator(cursor.peek().type());

                if (operator == null || precedence > operator.getPrecedence()) {
                    break;
                }

                cursor.next();

                ExpressionNode right = parseExpression(cursor, context, operator.getPrecedence() + 1);

                left = new BinaryOperation(left, operator, right);
            }

            return left;
        }

        throw new ParseException("No root parser found");
    }

}
