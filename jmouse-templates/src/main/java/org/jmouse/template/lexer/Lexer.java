package org.jmouse.template.lexer;

import java.util.ListIterator;
import java.util.function.Predicate;

import static org.jmouse.template.lexer.StandardToken.*;

/**
 * Defines a lexical analyzer (lexer) that provides token-based navigation and manipulation.
 *
 * <p>A lexer allows bidirectional movement over a list of tokens while supporting lookahead
 * and predicate-based traversal.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Lexer extends ListIterator<Entry>, Iterable<Entry> {

    /**
     * Returns the current token entry.
     *
     * @return the current token entry
     */
    Entry current();

    /**
     * Returns the total length of tokens.
     *
     * @return the number of tokens in the lexer
     */
    int length();

    /**
     * Returns the current cursor position.
     *
     * @return the cursor position
     */
    int cursor();

    /**
     * Sets the cursor to the specified position.
     *
     * @param cursor the new cursor position
     */
    void cursor(int cursor);

    /**
     * Looks for an entry between the specified start and end tokens.
     *
     * @param start the starting token
     * @param end   the ending token
     * @return the matching entry, or {@code null} if not found
     */
    Entry lookOver(Token start, Token end);

    /**
     * Moves forward while the given predicate condition is met.
     *
     * @param predicate the predicate to test entries
     */
    void forward(Predicate<Entry> predicate);

    /**
     * Moves backward while the given predicate condition is met.
     *
     * @param predicate the predicate to test entries
     */
    void backward(Predicate<Entry> predicate);

    /**
     * Moves forward to the specified entry.
     *
     * @param entry the entry to move forward to
     */
    void forward(Entry entry);

    /**
     * Moves backward to the specified entry.
     *
     * @param entry the entry to move backward to
     */
    void backward(Entry entry);

    /**
     * Moves forward to the specified token.
     *
     * @param token the token to move forward to
     */
    void forward(Token token);

    /**
     * Moves backward to the specified token.
     *
     * @param token the token to move backward to
     */
    void backward(Token token);

    /**
     * Creates a new lexer instance with an increased position.
     *
     * @param increase the position offset for the new lexer
     * @return a new lexer instance
     */
    Lexer lexer(int increase);

    /**
     * Checks if the current sequence of tokens matches the given tokens.
     *
     * @param limit  the number of tokens to check
     * @param offset the offset position from the cursor
     * @param tokens the expected token sequence
     * @return {@code true} if the sequence matches, otherwise {@code false}
     */
    boolean is(int limit, int offset, Token... tokens);

    /**
     * Moves forward if the next token matches the given token and value.
     *
     * @param token the expected token
     * @param value the expected value
     */
    default void forward(Token token, String value) {
        forward(Entry.of(token, value));
    }

    /**
     * Moves backward if the previous token matches the given token and value.
     *
     * @param token the expected token
     * @param value the expected value
     */
    default void backward(Token token, String value) {
        backward(Entry.of(token, value));
    }

    /**
     * Checks if the next token matches the given sequence.
     *
     * @param limit  the number of tokens to check
     * @param tokens the expected tokens
     * @return {@code true} if the sequence matches, otherwise {@code false}
     */
    default boolean is(int limit, Token... tokens) {
        return is(limit, 0, tokens);
    }

    /**
     * Checks if the current token matches the given tokens.
     *
     * @param tokens the expected tokens
     * @return {@code true} if the current token matches, otherwise {@code false}
     */
    default boolean isCurrent(Token... tokens) {
        return is(1, tokens);
    }

    /**
     * Checks if the next token matches the given tokens.
     *
     * @param tokens the expected tokens
     * @return {@code true} if the next token matches, otherwise {@code false}
     */
    default boolean isNext(Token... tokens) {
        return is(1, 1, tokens);
    }

    /**
     * Moves forward if the next token matches the specified token.
     *
     * @param token the token to check
     * @return {@code true} if moved forward, otherwise {@code false}
     */
    default boolean moveNext(Token token) {
        boolean isNext = isNext(token);

        if (isNext) {
            next();
        }

        return isNext;
    }

    /**
     * Checks if the previous token matches the given tokens.
     *
     * @param tokens the expected tokens
     * @return {@code true} if the previous token matches, otherwise {@code false}
     */
    default boolean isPrevious(Token... tokens) {
        return is(1, -1, tokens);
    }

    /**
     * Checks if a sequence of tokens appears in order.
     *
     * @param tokens the expected sequence of tokens
     * @return {@code true} if the sequence matches, otherwise {@code false}
     */
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

    /**
     * Returns a new lexer instance at the current position.
     *
     * @return a new lexer instance
     */
    default Lexer lexer() {
        return lexer(0);
    }

    /**
     * Retrieves the token entry at a given offset.
     *
     * @param offset the offset from the current position
     * @return the entry at the given offset
     */
    default Entry lookAhead(int offset) {
        return lexer(offset).current();
    }

    /**
     * Retrieves the next token entry.
     *
     * @return the next entry
     */
    default Entry lookAhead() {
        return lookAhead(1);
    }

    /**
     * Evaluates the lexer state against a custom condition.
     *
     * @param checker the condition to evaluate
     * @return {@code true} if the condition matches, otherwise {@code false}
     */
    default boolean check(Checker checker) {
        return checker.test(this);
    }

    /**
     * Functional interface for custom lexer conditions.
     */
    interface Checker extends Predicate<Lexer> {

        /**
         * Checks if the next token is a mathematical operator.
         */
        Checker MATH_TESTER = lexer -> lexer.isNext(T_PLUS, T_MINUS, T_DIVIDE, T_MULTIPLY);
    }

}
