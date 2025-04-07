package org.jmouse.el.parser.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

public class ParenthesesParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (!cursor.isNext(BasicToken.T_CLOSE_PAREN)) {
            cursor.ensure(BasicToken.T_OPEN_PAREN);
            parent.add(context.getParser(context.getOptions().nextParser()).parse(cursor, context));
            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        } else {
            cursor.ensure(BasicToken.T_OPEN_PAREN);
            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        }
    }

}
