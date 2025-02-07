package org.jmouse.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.expression.ast.PathVariableNode;
import org.jmouse.expression.ast.ValuesNode;

import static org.jmouse.expression.ExtendedToken.T_PATH_VARIABLE;

public class PathVariableParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        ValuesNode valuesNode = new ValuesNode();

        do {
            valuesNode.addElement(new PathVariableNode(lexer.current().value()));
        } while (lexer.moveNext(T_PATH_VARIABLE));

        parent.add(valuesNode);
    }

}
