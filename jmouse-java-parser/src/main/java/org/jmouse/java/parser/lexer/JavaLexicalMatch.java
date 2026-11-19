package org.jmouse.java.parser.lexer;

import java.util.Objects;
import java.util.function.Consumer;

/** A consuming lexical contribution may emit tokens, trivia, diagnostics, or an ignored region. */
public record JavaLexicalMatch(int length, Consumer<JavaLexicalOutput> emission) {
    public JavaLexicalMatch {
        if (length <= 0) {
            throw new IllegalArgumentException("A lexical rule must consume input");
        }
        Objects.requireNonNull(emission, "emission");
    }
}
