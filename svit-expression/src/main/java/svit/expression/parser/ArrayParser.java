package svit.expression.parser;

import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;

import static svit.ast.token.DefaultToken.*;

public class ArrayParser implements Parser {

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, T_OPEN_CURLY_BRACE);

        if (!lexer.isNext(T_CLOSE_CURLY_BRACE)) {
            parent.add(context.getParser(CommaSeparatedParser.class).parse(lexer, context));
        }

        shift(lexer, T_CLOSE_CURLY_BRACE);
    }

}
