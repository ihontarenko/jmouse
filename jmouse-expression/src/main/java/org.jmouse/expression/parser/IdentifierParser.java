package org.jmouse.expression.parser;

import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.expression.ast.IdentifierNode;
import org.jmouse.common.ast.token.DefaultToken;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;

public class IdentifierParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, DefaultToken.T_IDENTIFIER);
        IdentifierNode node = new IdentifierNode(lexer.current());
        node.setIdentifier(node.entry().value());
        parent.add(node);
    }

}
