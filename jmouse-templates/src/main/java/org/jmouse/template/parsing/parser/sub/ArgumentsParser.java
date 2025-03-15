package org.jmouse.template.parsing.parser.sub;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.ArgumentsNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.parser.OperatorParser;

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

