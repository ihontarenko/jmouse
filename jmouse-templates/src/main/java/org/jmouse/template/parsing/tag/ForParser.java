package org.jmouse.template.parsing.tag;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.parsing.TagParser;
import org.jmouse.template.parsing.ParserContext;

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
