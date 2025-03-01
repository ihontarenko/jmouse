package org.jmouse.template.parser;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.BasicNode;
import org.jmouse.template.node.LiteralNode;
import org.jmouse.template.node.Node;

public class ArithmeticParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(new LiteralNode(cursor.next().value()));
    }

}
