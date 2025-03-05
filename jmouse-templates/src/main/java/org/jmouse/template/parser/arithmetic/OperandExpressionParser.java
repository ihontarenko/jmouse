package org.jmouse.template.parser.arithmetic;

import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

public class OperandExpressionParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {

    }

    @Override
    public String getName() {
        return "operand";
    }

}
