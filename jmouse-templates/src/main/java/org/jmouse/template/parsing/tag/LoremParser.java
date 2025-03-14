package org.jmouse.template.parsing.tag;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.node.renderable.RawTextNode;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.TagParser;

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
