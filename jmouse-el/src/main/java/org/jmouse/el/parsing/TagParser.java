package org.jmouse.el.parsing;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.RenderableNode;

public interface TagParser {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

    String getName();

}
