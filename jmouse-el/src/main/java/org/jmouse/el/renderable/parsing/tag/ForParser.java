package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.ParserContext;

public class ForParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        Token token = cursor.ensure(TemplateToken.T_FOR);

        System.out.println(token);

        return null;
    }

    @Override
    public String getName() {
        return "for";
    }

}
