package svit.expression.parser;

import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ast.ValuesNode;

import static org.jmouse.common.ast.token.DefaultToken.*;

public class CommaSeparatedParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        ValuesNode valuesNode = new ValuesNode();

        do {
            valuesNode.addElement(
                    context.getParser(AnyExpressionParser.class).parse(lexer, context));

            lexer.moveNext(T_COMMA);

        } while (lexer.isCurrent(T_COMMA));

        parent.add(valuesNode);
    }

}
