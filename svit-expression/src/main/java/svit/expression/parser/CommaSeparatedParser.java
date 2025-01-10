package svit.expression.parser;

import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.expression.ast.ValuesNode;

import static svit.ast.token.DefaultToken.*;

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
