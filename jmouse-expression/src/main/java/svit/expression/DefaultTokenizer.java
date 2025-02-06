package svit.expression;

import org.jmouse.common.ast.token.Token;

public class DefaultTokenizer extends org.jmouse.common.ast.token.DefaultTokenizer {

    @Override
    public Token.Entry entry(Token token, String value, int position, int ordinal) {
        return super.entry(token, value, position, ordinal);
    }

}
