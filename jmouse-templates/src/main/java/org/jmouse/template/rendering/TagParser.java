package org.jmouse.template.rendering;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

public interface TagParser {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

    String getName();

}
