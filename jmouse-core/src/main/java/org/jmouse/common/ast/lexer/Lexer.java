package org.jmouse.common.ast.lexer;

import org.jmouse.common.ast.token.Token;

import java.util.ListIterator;
import java.util.function.Predicate;

import static org.jmouse.common.ast.token.DefaultToken.*;

public interface Lexer extends ListIterator<Token.Entry>, Iterable<Token.Entry> {

    Token.Entry current();

    int length();

    int cursor();

    void cursor(int cursor);

    Token.Entry lookOver(Token start, Token end);

    void forward(Predicate<Token.Entry> predicate);

    void backward(Predicate<Token.Entry> predicate);

    void forward(Token.Entry entry);

    void backward(Token.Entry entry);

    void forward(Token token);

    void backward(Token token);

    Lexer lexer(int increase);

    boolean is(int limit, int offset, Token... tokens);

    default void forward(Token token, String value) {
        forward(Token.Entry.of(token, value));
    }

    default void backward(Token token, String value) {
        backward(Token.Entry.of(token, value));
    }

    default boolean is(int limit, Token... tokens) {
        return is(limit, 0, tokens);
    }

    default boolean isCurrent(Token... tokens) {
        return is(1, tokens);
    }

    default boolean isNext(Token... tokens) {
        return is(1, 1, tokens);
    }

    default boolean moveNext(Token token) {
        boolean isNext = isNext(token);

        if (isNext) {
            next();
        }

        return isNext;
    }

    default boolean isPrevious(Token... tokens) {
        return is(1, -1, tokens);
    }

    default boolean sequence(Token... tokens) {
        Lexer lexer = lexer();

        for (Token token : tokens) {
            if (lexer.isNext(token)) {
                lexer.next();
            } else {
                return false;
            }
        }

        return true;
    }

    default Lexer lexer() {
        return lexer(0);
    }

    default Token.Entry lookAhead(int offset) {
        return lexer(offset).current();
    }

    default Token.Entry lookAhead() {
        return lookAhead(1);
    }

    default boolean check(Checker checker) {
        return checker.test(this);
    }

    interface Checker extends Predicate<Lexer> {

        Checker MATH_TESTER = lexer
                -> lexer.isNext(T_PLUS, T_MINUS, T_DIVIDE, T_MULTIPLY);

    }

}
