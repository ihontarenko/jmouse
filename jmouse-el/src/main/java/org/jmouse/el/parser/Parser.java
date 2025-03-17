package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;

public interface Parser {

    void parse(TokenCursor cursor, Node parent, ParserContext context);

    default Node parse(TokenCursor cursor, ParserContext context) {
        Node container = BasicNode.forToken(null);
        parse(cursor, container, context);
        return container.first();
    }

}
