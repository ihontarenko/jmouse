package svit.expression.parser;

import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import svit.expression.ast.LiteralNode;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.token.Token;

import static org.jmouse.common.ast.token.DefaultToken.*;

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
