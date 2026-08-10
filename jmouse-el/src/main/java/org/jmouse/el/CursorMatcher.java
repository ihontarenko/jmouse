package org.jmouse.el;

import org.jmouse.core.matcher.Matcher;
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

    public static Matcher<TokenCursor> bean() {
        return new BeanMatcher();
    }

    public static Matcher<TokenCursor> variableAlias() {
        return new VariableAliasMatcher();
    }

    public static Matcher<TokenCursor> placeholder() {
        return new PlaceholderMatcher();
    }

    public record PlaceholderMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(
                    T_DOLLAR, T_OPEN_CURLY, T_IDENTIFIER
            );
        }

    }

    public record VariableAliasMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(
                    T_DOLLAR, T_IDENTIFIER, T_COLON
            );
        }

    }

    public record BeanMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return methodCall(cursor) || fieldAccess(cursor) || constantAccess(cursor);
        }

        public boolean methodCall(TokenCursor cursor) {
            return cursor.matchesSequence(
                    T_AT, T_IDENTIFIER, T_DOT, T_IDENTIFIER, T_OPEN_PAREN
            );
        }

        public boolean fieldAccess(TokenCursor cursor) {
            return cursor.matchesSequence(
                    T_AT, T_IDENTIFIER, T_COLON, T_DOLLAR, T_IDENTIFIER
            );
        }

        public boolean constantAccess(TokenCursor cursor) {
            return cursor.matchesSequence(
                    T_AT, T_IDENTIFIER, T_HASH, T_IDENTIFIER
            );
        }

    }

    record LambdaMatcher() implements Matcher<TokenCursor> {

        @Override
        public boolean matches(TokenCursor cursor) {
            return cursor.matchesSequence(T_OPEN_PAREN, T_IDENTIFIER, T_CLOSE_PAREN)
                    || cursor.matchesSequence(T_OPEN_PAREN, T_IDENTIFIER, T_COLON)
                    || cursor.matchesSequence(T_OPEN_PAREN, T_IDENTIFIER, T_COMMA, T_IDENTIFIER)
                    || cursor.matchesSequence(T_IDENTIFIER, T_ARROW)
                    || cursor.matchesSequence(T_OPEN_PAREN, T_CLOSE_PAREN, T_ARROW);
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
            return cursor.matchesSequence(T_IDENTIFIER, T_OPEN_PAREN)
                    || cursor.matchesSequence(T_IDENTIFIER, T_COLON, T_IDENTIFIER, T_OPEN_PAREN);
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
