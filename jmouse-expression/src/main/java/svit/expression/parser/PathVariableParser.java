package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ast.PathVariableNode;
import svit.expression.ast.ValuesNode;

import static svit.expression.ExtendedToken.T_PATH_VARIABLE;

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
