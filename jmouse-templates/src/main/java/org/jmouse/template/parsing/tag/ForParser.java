package org.jmouse.template.parsing.tag;

import org.jmouse.template.TemplateToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.RenderableNode;
import org.jmouse.el.parsing.TagParser;
import org.jmouse.el.parsing.ParserContext;

public class ForParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        Token token = cursor.ensure(TemplateToken.T_FOR);

        return null;
    }

    @Override
    public String getName() {
        return "for";
    }

}
