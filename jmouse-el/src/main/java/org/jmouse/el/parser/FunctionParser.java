package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

/**
 * Parses function calls and their arguments.
 *
 * <p>Handles cases such as:</p>
 * <ul>
 *     <li>Function without arguments: {@code emptyFunction()}</li>
 *     <li>Function with multiple arguments: {@code sum(1, 2, x)}</li>
 *     <li>Function with nested expressions: {@code max(1, min(2, 3))}</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        // Extract function name and ensure it's a valid identifier
        FunctionNode function = new FunctionNode(cursor.peek().value());

        // Ensure that the current token is a function name
        cursor.ensure(BasicToken.T_IDENTIFIER);

        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
            function.setArguments(context.getParser(ParenthesesParser.class).parse(cursor, context));
            context.clearOptions();
        }

        parent.add(function);
    }

}
