package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.RenderableNode;

public interface TagParser {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

    String getName();

}
