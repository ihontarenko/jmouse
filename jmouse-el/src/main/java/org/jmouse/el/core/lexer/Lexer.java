package org.jmouse.el.core.lexer;

public interface Lexer {

    TokenCursor tokenize(CharSequence text);

}
