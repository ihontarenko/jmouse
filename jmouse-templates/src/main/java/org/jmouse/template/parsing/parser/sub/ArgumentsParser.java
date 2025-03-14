package org.jmouse.template.parsing.parser.sub;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.expression.ArgumentsNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.parser.OperatorParser;

import static org.jmouse.template.lexer.BasicToken.T_CLOSE_PAREN;
import static org.jmouse.template.lexer.BasicToken.T_OPEN_PAREN;

public class ArgumentsParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node   arguments = new ArgumentsNode();
        Parser parser    = context.getParser(OperatorParser.class);

//        cursor.ensure(T_OPEN_PAREN);

        do {
            Node argument = parser.parse(cursor, context);
            arguments.add(argument);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);

//        cursor.ensure(T_CLOSE_PAREN);

        parent.add(arguments);
    }

}

