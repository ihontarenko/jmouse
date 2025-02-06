package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.common.ast.token.DefaultToken;
import svit.expression.ExtendedToken;
import svit.expression.ast.ObjectMethodNode;

public class ObjectMethodCallParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, ExtendedToken.T_VARIABLE);

        ObjectMethodNode node       = new ObjectMethodNode();
        String           objectName = lexer.current().value().substring(1);

        shift(lexer, DefaultToken.T_DOT);
        shift(lexer, DefaultToken.T_IDENTIFIER);

        String methodName = lexer.current().value();

        node.setObjectName(objectName);
        node.setMethodName(methodName);

        shift(lexer, DefaultToken.T_OPEN_BRACE);

        if (!lexer.isNext(DefaultToken.T_CLOSE_BRACE)) {
            node.setArguments(context.getParser(CommaSeparatedParser.class).parse(lexer, context));
        }

        shift(lexer, DefaultToken.T_CLOSE_BRACE);

        parent.add(node);
    }

}
