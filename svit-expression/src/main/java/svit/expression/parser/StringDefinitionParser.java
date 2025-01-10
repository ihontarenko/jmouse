package svit.expression.parser;

import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.expression.ast.StringDefinitionNode;

import static svit.ast.token.DefaultToken.T_DIVIDE;

public class StringDefinitionParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        StringDefinitionNode node = new StringDefinitionNode();

        node.setHandler(context.getParser(VariableParser.class).parse(lexer, context));
        shift(lexer, T_DIVIDE);
        node.setCommand(context.getParser(IdentifierParser.class).parse(lexer, context));

        parent.add(node);
    }

}
