package org.jmouse.template.parsing.parser;

import org.jmouse.template.extension.Function;
import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.FunctionNode;
import org.jmouse.template.parsing.ParseException;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.parser.sub.ArgumentsParser;

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
        Function     executor = context.getFunction(function.getName());

        if (executor == null) {
            throw new ParseException("Unknown function: " + function.getName());
        }

        // Ensure that the current token is a function name
        cursor.ensure(BasicToken.T_IDENTIFIER);

        // Parse function arguments
        if (!cursor.isNext(BasicToken.T_CLOSE_PAREN)) {
            cursor.ensure(BasicToken.T_OPEN_PAREN);
            Parser parser = context.getParser(ArgumentsParser.class);
            function.setArguments(parser.parse(cursor, context));
            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        } else {
            cursor.ensure(BasicToken.T_OPEN_PAREN);
            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        }

        parent.add(function);
    }

}
