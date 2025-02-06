package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ast.StringDefinitionNode;

import static org.jmouse.common.ast.token.DefaultToken.T_DIVIDE;

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
