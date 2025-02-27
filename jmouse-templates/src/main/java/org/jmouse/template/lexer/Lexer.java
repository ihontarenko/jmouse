package org.jmouse.template.lexer;

public interface Lexer {

    TokenCursor tokenize(CharSequence text);

}
