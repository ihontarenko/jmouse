package org.jmouse.template.parser;

import org.jmouse.template.NameKeeper;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.RenderableNode;

public interface ExpressionParser extends NameKeeper {

    RenderableNode parse(TokenCursor cursor, ParserContext context);

}
