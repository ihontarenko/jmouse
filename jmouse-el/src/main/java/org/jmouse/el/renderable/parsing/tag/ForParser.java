package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.core.node.Node;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.parser.TagParser;
import org.jmouse.el.core.parser.ParserContext;

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
