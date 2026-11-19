package org.jmouse.el.lexer;

import java.util.HashSet;
import java.util.Set;

/** Immutable grouping vocabulary for a token stream; a language supplies its own token types. */
public record TokenNesting(Set<Token.Type> opening, Set<Token.Type> closing) {
    public TokenNesting {
        opening = Set.copyOf(opening);
        closing = Set.copyOf(closing);
        Set<Token.Type> overlap = new HashSet<>(opening);
        overlap.retainAll(closing);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("A token cannot both open and close a nesting region");
        }
    }

    public int depthChange(Token.Type type) {
        return opening.contains(type) ? 1 : closing.contains(type) ? -1 : 0;
    }

    public static TokenNesting standard() {
        return new TokenNesting(Set.of(BasicToken.T_OPEN_PAREN, BasicToken.T_OPEN_BRACKET),
                Set.of(BasicToken.T_CLOSE_PAREN, BasicToken.T_CLOSE_BRACKET));
    }
}
