package org.jmouse.template.parsing.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.rendering.RenderableNode;
import org.jmouse.template.node.RawTextNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;

public class LoremParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(BasicToken.T_IDENTIFIER);

        return new RawTextNode("Lorem ipsum");
    }

    @Override
    public String getName() {
        return "lorem";
    }

}
