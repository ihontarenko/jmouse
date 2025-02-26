package org.jmouse.template.lexer;

import java.util.List;

public interface Tokenizer {

    List<Token> tokenize(CharSequence text);

}
