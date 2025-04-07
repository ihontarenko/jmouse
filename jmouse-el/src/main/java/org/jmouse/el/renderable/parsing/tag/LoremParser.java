package org.jmouse.el.renderable.parsing.tag;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.renderable.node.RawTextNode;
import org.jmouse.el.core.parser.ParserContext;
import org.jmouse.el.core.parser.TagParser;

public class LoremParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(BasicToken.T_IDENTIFIER);

        return new RawTextNode("Lorem ipsum");
    }

    @Override
    public String getName() {
        return "lorem";
    }

}
