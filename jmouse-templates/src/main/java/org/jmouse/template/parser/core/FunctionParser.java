package org.jmouse.template.parser.core;

import org.jmouse.template.extension.Function;
import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.ValuesNode;
import org.jmouse.template.node.expression.FunctionNode;
import org.jmouse.template.parser.ParseException;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

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
        // 📌 Extract function name and ensure it's a valid identifier
        FunctionNode function = new FunctionNode(cursor.peek().value());

        cursor.ensure(BasicToken.T_IDENTIFIER); // Ensure that the current token is a function name
        cursor.expect(BasicToken.T_OPEN_PAREN); // Expect '('

        Function executor = context.getFunction(function.getName());

        if (executor == null) {
            throw new ParseException("Unknown function: " + function.getName());
        }

        // 📝 Parse function arguments
        if (!cursor.isNext(BasicToken.T_CLOSE_PAREN)) {
            Parser parser    = context.getParser(OperatorParser.class);
            Node   arguments = new ValuesNode();

            do {
                cursor.next();
                arguments.add(parser.parse(cursor, context));
            } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.hasNext());

            function.setArguments(arguments);

            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        } else {
            cursor.expect(BasicToken.T_CLOSE_PAREN);
        }

        // consume close parentheses
        cursor.next();

        parent.add(function);
    }

}
