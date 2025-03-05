package org.jmouse.template.parser;

import org.jmouse.template.NameKeeper;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.BasicNode;
import org.jmouse.template.node.Node;

public interface Parser extends NameKeeper {

    void parse(TokenCursor cursor, Node parent, ParserContext context);

    default Node parse(TokenCursor cursor, ParserContext context) {
        Node container = BasicNode.forToken(null);
        parse(cursor, container, context);
        return container.first();
    }

}
