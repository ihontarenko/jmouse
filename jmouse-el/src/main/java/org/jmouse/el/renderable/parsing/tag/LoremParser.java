package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.rendering.RenderableNode;
import org.jmouse.el.renderable.node.RawTextNode;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;

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
