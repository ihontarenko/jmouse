package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;

public interface TagParser {

    Node parse(TokenCursor cursor, ParserContext context);

    String getName();

}
