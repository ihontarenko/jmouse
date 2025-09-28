package org.jmouse.el;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import java.util.Arrays;

import static org.jmouse.el.lexer.BasicToken.*;

public class CursorMatcher {

    public static Matcher<TokenCursor> literal() {
        return new LiteralMatcher();
    }

    public static Matcher<TokenCursor> lambda() {
        return new LambdaMatcher();
    }

    public static Matcher<TokenCursor> function() {
        return new FunctionMatcher();
    }

    public static Matcher<TokenCursor> sequence(Token.Type... types) {
        return new SequnceMatcher(types);
    }

    record LambdaMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(BasicToken.T_OPEN_PAREN, T_IDENTIFIER, T_CLOSE_PAREN)
                    || cursor.matchesSequence(BasicToken.T_OPEN_PAREN, T_IDENTIFIER, T_COLON)
                    || cursor.matchesSequence(BasicToken.T_OPEN_PAREN, T_IDENTIFIER, T_COMMA, T_IDENTIFIER)
                    || cursor.matchesSequence(BasicToken.T_IDENTIFIER, T_ARROW)
                    || cursor.matchesSequence(BasicToken.T_OPEN_PAREN, T_CLOSE_PAREN, T_ARROW);
        }

    }

    record LiteralMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.isCurrent(T_NUMERIC, T_STRING, T_TRUE, T_FALSE, T_NULL,
                                    T_BYTE, T_SHORT, T_CHARACTER, T_INT, T_LONG, T_FLOAT, T_DOUBLE);
        }

    }

    record FunctionMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_IDENTIFIER, BasicToken.T_OPEN_PAREN)
                    || cursor.matchesSequence(T_IDENTIFIER, T_COLON, T_IDENTIFIER, BasicToken.T_OPEN_PAREN);
        }

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
