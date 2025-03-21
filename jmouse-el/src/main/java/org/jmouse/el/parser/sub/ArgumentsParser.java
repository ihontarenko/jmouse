package org.jmouse.el.parser.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

/**
 * Parses a comma-separated list of arguments.
 * <p>
 * This parser uses an {@link ExpressionParser} (from the context) to parse each argument
 * and aggregates them into an {@link ArgumentsNode}.
 * </p>
 */
public class ArgumentsParser implements Parser {

    /**
     * Parses arguments from the token stream and adds them to the given parent node.
     *
     * @param cursor  the token cursor
     * @param parent  the node to which parsed arguments are added
     * @param context the parser context for obtaining sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Parser parser = context.getParser(ExpressionParser.class);
        do {
            Node argument = parser.parse(cursor, context);
            parent.add(argument);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);
    }

    /**
     * Parses arguments and returns them as an {@link ArgumentsNode}.
     *
     * @param cursor  the token cursor
     * @param context the parser context for obtaining sub-parsers
     * @return an ArgumentsNode containing the parsed arguments
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        Node arguments = new ArgumentsNode();
        parse(cursor, arguments, context);
        return arguments;
    }
}
