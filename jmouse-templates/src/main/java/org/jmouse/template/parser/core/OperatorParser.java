package org.jmouse.template.parser.core;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.ExpressionNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.BinaryOperation;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

import static org.jmouse.template.lexer.BasicToken.T_OPEN_PAREN;

/**
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OperatorParser implements Parser {

    /**
     * Parses an arithmetic tag and adds it as a child of the given parent node.
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
     * Parses an arithmetic tag.
     *
     * @param cursor the token cursor
     * @return the parsed tag node
     */
    private ExpressionNode parseExpression(TokenCursor cursor, ParserContext context, int precedence) {
        ExpressionParser parser = (ExpressionParser) context.getParser(ExpressionParser.class);
        ExpressionNode   left   = (ExpressionNode) parser.parse(cursor, context);
        Token            token  = cursor.peek();

        while (cursor.hasNext()) {
            Operator operator = context.getOperator(token.type());

            if (operator == null && cursor.isCurrent(T_OPEN_PAREN)) {
                System.out.println(cursor);
            }

            if (operator == null || precedence > operator.getPrecedence()) {
                break;
            }

            cursor.next();

            ExpressionNode right = parseExpression(cursor, context, operator.getPrecedence() + 1);

            left = new BinaryOperation(left, operator, right);
        }

        return left;
    }

}
