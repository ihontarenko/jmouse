package svit.expression;

import svit.ast.lexer.AbstractLexer;
import svit.ast.token.Token;

import java.util.List;

public class DefaultLexer extends AbstractLexer {

    public DefaultLexer(List<Token.Entry> entries) {
        super(entries);
    }

}
