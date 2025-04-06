package org.jmouse.el.core;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.core.lexer.Token;
import org.jmouse.el.core.lexer.TokenCursor;

import java.util.Arrays;

public class CursorMatcher {

    public static Matcher<TokenCursor> sequence(Token.Type... types) {
        return new SequnceMatcher(types);
    }

    record SequnceMatcher(Token.Type... types) implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(types);
        }

        @Override
        public String toString() {
            return "Sequence%s".formatted(Arrays.toString(types));
        }
    }

}
