package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.BasicNode;
import org.jmouse.el.core.node.Node;

public interface Parser {

    void parse(TokenCursor cursor, Node parent, ParserContext context);

    default Node parse(TokenCursor cursor, ParserContext context) {
        Node container = BasicNode.forToken(null);
        parse(cursor, container, context);
        return container.first();
    }

}
