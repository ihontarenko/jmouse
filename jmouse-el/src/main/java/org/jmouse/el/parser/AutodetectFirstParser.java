package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;

public class AutodetectFirstParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (context.getParser(cursor) instanceof Parser parser) {
            parent.add(parser.parse(cursor, context));
        } else {
            parent.add(context.getParser(OperatorParser.class).parse(cursor, context));
        }
    }

}
