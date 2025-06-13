package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.URLNode;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.el.renderable.lexer.TemplateToken.T_URL;

public class URLParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(T_URL);

        URLNode node = new URLNode();
        node.setRawURL(cursor.ensure(T_STRING).value());

        return node;
    }

    @Override
    public String getName() {
        return "url";
    }

}
