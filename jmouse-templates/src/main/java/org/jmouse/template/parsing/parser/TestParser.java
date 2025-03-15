package org.jmouse.template.parsing.parser;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.FunctionNode;
import org.jmouse.template.node.expression.TestNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.ParserOptions;
import org.jmouse.template.parsing.parser.sub.ArgumentsParser;
import org.jmouse.template.parsing.parser.sub.ParenthesesParser;

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
        cursor.ensure(BasicToken.T_IDENTIFIER);

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
            test.setArguments(context.getParser(ParenthesesParser.class).parse(cursor, context));
            context.clearOptions();
        }

        parent.add(test);
    }

}
