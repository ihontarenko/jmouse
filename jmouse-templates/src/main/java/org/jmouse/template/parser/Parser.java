package org.jmouse.template.parser;

import org.jmouse.template.lexer.Lexer;
import org.jmouse.template.node.EntryNode;
import org.jmouse.template.node.Node;

public interface Parser {

    void parse(Lexer lexer, Node parent, ParserContext context);

    default Node parse(Lexer lexer, ParserContext context) {
        Node node = new EntryNode();

        parse(lexer, node, context);

        return node.first();
    }

}
