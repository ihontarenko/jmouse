package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ExtendedToken;
import svit.expression.ast.VariableNode;

public class VariableParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, ExtendedToken.T_VARIABLE);
        parent.add(new VariableNode(lexer.current().value()));
    }

}
