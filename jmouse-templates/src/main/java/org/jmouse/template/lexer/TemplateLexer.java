package org.jmouse.template.lexer;

import org.jmouse.template.StringSource;

import java.util.List;

public class TemplateLexer implements Lexer {

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenCursor tokenCursor = null;

        if (text instanceof StringSource source) {
            DefaultTokenizer tokenizer = new DefaultTokenizer();
            List<Token>      tokens    = tokenizer.tokenize(source);


        }

        return tokenCursor;
    }

}
