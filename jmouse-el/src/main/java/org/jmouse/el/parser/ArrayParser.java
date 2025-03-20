package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArrayNode;
import org.jmouse.el.parser.sub.ArgumentsParser;

/**
 * Parses an array expression.
 * <p>
 * Ensures the array starts with '[' and ends with ']'. If not empty, it delegates element parsing
 * to the {@link ArgumentsParser} and wraps the results in an {@link ArrayNode}.
 * </p>
 */
public class ArrayParser implements Parser {

    /**
     * Parses an array expression from the token stream.
     *
     * @param cursor  the token stream cursor
     * @param parent  the parent node to which the parsed array node is added
     * @param context the parser context for retrieving sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node array = new ArrayNode();

        cursor.ensure(BasicToken.T_OPEN_BRACKET);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
            context.getParser(ArgumentsParser.class).parse(cursor, array, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_BRACKET);

        parent.add(array);
    }
}
