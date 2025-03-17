package org.jmouse.el.lexer;

public interface Lexer {

    TokenCursor tokenize(CharSequence text);

}
