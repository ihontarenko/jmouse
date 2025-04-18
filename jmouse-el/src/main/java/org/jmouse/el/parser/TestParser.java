package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.TestNode;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

/**
 * Parses tests calls and their arguments if present.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TestParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        boolean negated = false;

        if (cursor.isCurrent(BasicToken.T_NE)) {
            cursor.ensure(BasicToken.T_NE);
            negated = true;
        }

        // Extract test name and ensure it's a valid identifier
        TestNode test = new TestNode(cursor.peek().value());
        test.setNegated(negated);

        // Ensure that the current token is a test name
        cursor.ensure(BasicToken.T_IDENTIFIER, BasicToken.T_NULL);

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
            test.setArguments((ExpressionNode) context.getParser(ParenthesesParser.class).parse(cursor, context));
            context.clearOptions();
        }

        parent.add(test);
    }

}
