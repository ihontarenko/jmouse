package org.jmouse.template.parsing.tag;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.ParserContext;

public class ForParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        Token token = cursor.ensure(TemplateToken.T_FOR);

        System.out.println(token);

        return null;
    }

    @Override
    public String getName() {
        return "for";
    }

}
