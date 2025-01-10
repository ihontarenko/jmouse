package svit.expression.parser;

import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.expression.ast.LiteralNode;
import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.token.Token;

import static svit.ast.token.DefaultToken.*;

public class LiteralParser implements Parser {

    public static final Token[] TOKENS = {T_INT, T_FLOAT, T_STRING, T_TRUE, T_FALSE, T_NULL};

    @Override
    public void parse(Lexer lexer, Node parent, ParserContext context) {
        shift(lexer, TOKENS);

        Token.Entry entry   = lexer.current();
        LiteralNode literal = new LiteralNode(entry);

        literal.setValue(entry.value());

        if (entry.token() instanceof Enum<?> tokenName) {
            literal.setAttribute("type", tokenName.name());
        }

        parent.add(literal);
    }

}
