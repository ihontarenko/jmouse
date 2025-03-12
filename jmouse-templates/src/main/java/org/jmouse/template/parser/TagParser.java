package org.jmouse.template.parser;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;

public interface TagParser {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

    String getName();

}
