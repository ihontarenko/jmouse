package svit.expression.parser;

import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
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
