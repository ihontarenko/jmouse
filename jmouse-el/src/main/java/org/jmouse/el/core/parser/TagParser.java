package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;

public interface TagParser {

    Node parse(TokenCursor cursor, ParserContext context);

    String getName();

}
