package org.jmouse.template.parsing.parser.sub;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.ParserContext;

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
