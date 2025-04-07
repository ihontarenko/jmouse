package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.node.TextNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;

public class LoremParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(BasicToken.T_IDENTIFIER);

        return new TextNode("Lorem ipsum");
    }

    @Override
    public String getName() {
        return "lorem";
    }

}
