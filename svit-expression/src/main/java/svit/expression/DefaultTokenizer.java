package svit.expression;

import svit.ast.token.Token;

public class DefaultTokenizer extends svit.ast.token.DefaultTokenizer {

    @Override
    public Token.Entry entry(Token token, String value, int position, int ordinal) {
        return super.entry(token, value, position, ordinal);
    }

}
