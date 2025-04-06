package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.FilterNode;
import org.jmouse.el.core.parser.sub.ArgumentsParser;
import org.jmouse.el.core.parser.sub.ParenthesesParser;

/**
 * Parses filters calls and their arguments if present.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FilterParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Extract filter name and ensure it's a valid identifier
        FilterNode filter = new FilterNode(cursor.peek().value());

        // Ensure that the current token is a filter name
        cursor.ensure(BasicToken.T_IDENTIFIER);

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
            filter.setArguments((ExpressionNode) context.getParser(ParenthesesParser.class).parse(cursor, context));
            context.clearOptions();
        }

        parent.add(filter);
    }

}
