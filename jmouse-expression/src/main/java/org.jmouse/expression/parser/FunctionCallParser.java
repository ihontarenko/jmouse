package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.common.ast.token.DefaultToken;
import svit.expression.ExtendedToken;
import svit.expression.ast.FunctionNode;

public class FunctionCallParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, ExtendedToken.T_VARIABLE);

        FunctionNode node       = new FunctionNode();
        String       methodName = lexer.current().value().substring(1);

        node.setMethodName(methodName);

        shift(lexer, DefaultToken.T_OPEN_BRACE);

        if (!lexer.isNext(DefaultToken.T_CLOSE_BRACE)) {
            node.setArguments(context.getParser(CommaSeparatedParser.class).parse(lexer, context));
        }

        shift(lexer, DefaultToken.T_CLOSE_BRACE);

        parent.add(node);
    }

}
