package org.jmouse.template.parser.tag;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.parser.TagParser;
import org.jmouse.template.parser.ParserContext;

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
