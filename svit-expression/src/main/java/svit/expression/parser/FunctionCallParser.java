package svit.expression.parser;

import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.ast.token.DefaultToken;
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
