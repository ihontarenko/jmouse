package org.jmouse.el.parsing.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parsing.Parser;
import org.jmouse.el.parsing.ParserContext;

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
