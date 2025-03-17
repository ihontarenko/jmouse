package org.jmouse.el.parsing.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.parsing.Parser;
import org.jmouse.el.parsing.ParserContext;
import org.jmouse.el.parsing.OperatorParser;

public class ArgumentsParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node   arguments = new ArgumentsNode();
        Parser parser    = context.getParser(OperatorParser.class);

        do {
            Node argument = parser.parse(cursor, context);
            arguments.add(argument);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);

        parent.add(arguments);
    }

}

