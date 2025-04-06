package org.jmouse.el.core.parser.sub;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.parser.Parser;
import org.jmouse.el.core.parser.ParserContext;

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
