package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.rendering.RenderableNode;

public interface TagParser {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

    String getName();

}
