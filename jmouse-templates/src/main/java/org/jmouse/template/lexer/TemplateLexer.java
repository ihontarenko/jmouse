package org.jmouse.template.lexer;

import org.jmouse.el.lexer.*;

import java.util.List;

public class TemplateLexer implements Lexer {

    @Override
    public TokenCursor tokenize(CharSequence text) {
        TokenCursor tokenCursor = null;

        if (text instanceof TokenizableSource source) {
            TemplateTokenizer tokenizer = new TemplateTokenizer();
            List<Token>       tokens    = tokenizer.tokenize(source);

            tokenCursor = new DefaultTokenCursor(source, tokens);
        }

        return tokenCursor;
    }

}
