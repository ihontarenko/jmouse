package org.jmouse.el.lexer;

import org.jmouse.core.Streamable;

import java.util.List;

/**
 * TokenCursor provides an advanced API for navigating a stream of tokens.
 * It offers methods for lookahead, shifting, retracting, expectation checking,
 * and managing savepoints for backtracking.
 *
 * <p>This interface is intended to be used by parsers to inspect and consume tokens
 * with support for backtracking and lookahead. It is built on top of an underlying token list.</p>
 */
public interface TokenCursor extends Streamable<Token> {

    /**
     * Returns the source these tokens were read from, when the cursor knows it.
     *
     * <p>Tokens carry an offset but not the text around it, so a parser that needs the source
     * verbatim — to quote a slice of it, or to turn an offset into a line and column — has no way
     * back to the characters. This is that way back.</p>
     *
     * <p>The default implementation returns {@code null}: a cursor is free not to have a source,
     * and callers are expected to degrade rather than fail when it does not.</p>
     *
     * @return the tokenizable source, or {@code null} when the cursor has none
     */
    default TokenizableSource source() {
        return null;
    }

    /**
     * Returns true if there is at least one more token in the stream.
     *
     * @return true if tokens remain, false otherwise
     */
    boolean hasNext();

    /**
     * Returns true if there is at least one token before the current position.
     *
     * @return true if there are previous tokens, false otherwise
     */
    boolean hasPrevious();

    /**
     * Consumes and returns the next token in the stream.
     *
     * @return the next Token object (including its value, position, and type)
     */
    Token next();

    /**
     * Attempts to consume the current token if it matches any of the expected types.
     * If the current token matches one of the provided types, it is consumed and the cursor advances.
     * Otherwise, the cursor remains unchanged.
     *
     * @param expected an array of token types to check against
     * @return {@code true} if the token was consumed, {@code false} otherwise
     */
    default boolean consumeIf(Token.Type... expected) {
        boolean advanced = false;

        if (isCurrent(expected)) {
            next();
            advanced = true;
        }

        return advanced;
    }

    /**
     * Consumes and returns the previous token in the stream.
     *
     * @return the previous Token object (including its value, position, and type)
     */
    Token previous();

    /**
     * Returns the next token without consuming it.
     *
     * @return the next Token object
     */
    Token peek();

    /**
     * Returns the token at the specified offset from the current position without consuming any tokens.
     * For example, lookAt(0) is equivalent to peek(), lookAt(1) returns the token immediately after peek(), etc.
     *
     * @param offset the offset from the current position (0-based)
     * @return the Token object at the given offset
     */
    Token lookAt(int offset);

    /**
     * Looks for the first token after a token of the given type
     * without moving the cursor.
     *
     * @param type the token type to look after
     * @return the token immediately following the matching token,
     *         or {@code null} if no such token exists
     */
    Token lookAfter(Token.Type type);

    /**
     * Looks for the first token before a token of the given type
     * without moving the cursor.
     *
     * @param type the token type to look before
     * @return the token immediately preceding the matching token,
     *         or {@code null} if no such token exists
     */
    Token lookBefore(Token.Type type);

    /**
     * Advances the cursor by the specified number of tokens (consumes them).
     *
     * @param count the number of tokens to shift
     */
    void shift(int count);

    /**
     * Moves the cursor backwards by the specified number of tokens, effectively "retracting" the stream.
     *
     * @param count the number of tokens to retract
     * @throws IllegalStateException if retracting more tokens than have been consumed
     */
    void retract(int count);

    /**
     * Consumes the next token and verifies that its type matches the expected type.
     * If the token's type does not match, a SyntaxErrorException is thrown.
     *
     * @param expected the expected Token.Type
     * @return the consumed Token object
     * @throws SyntaxErrorException if the next token's type does not match the expected type
     */
    Token expect(Token.Type... expected) throws SyntaxErrorException;

    /**
     * Expects a specific token type at the current position and advances if found.
     *
     * @param expected the expected token type
     * @return the current token if it matches the expected type
     * @throws SyntaxErrorException if the current token does not match the expected type
     */
    Token ensure(Token.Type... expected) throws SyntaxErrorException;

    /**
     * Checks whether the current token matches the specified expected type.
     *
     * @param expected the expected Token.Type for the current token
     * @return true if the current token matches, false otherwise
     */
    default boolean isCurrent(Token.Type... expected) {
        return checkAt(0, expected);
    }

    /**
     * Checks whether the previous token matches the specified expected type.
     *
     * @param expected the expected Token.Type for the previous token
     * @return true if the previous token matches, false otherwise
     */
    default boolean isPrevious(Token.Type... expected) {
        return checkAt(-1, expected);
    }

    /**
     * Checks whether the next token matches the specified expected type.
     *
     * @param expected the expected Token.Type for the next token
     * @return true if the next token matches, false otherwise
     */
    default boolean isNext(Token.Type... expected) {
        return checkAt(1, expected);
    }

    /**
     * Checks whether the tokens starting at the current position match the specified sequence of token types.
     *
     * @param expected an array of expected token types, in the desired order
     * @return {@code true} if the tokens from the current position match the expected sequence, {@code false} otherwise
     */
    default boolean matchesSequence(Token.Type... expected) {
        boolean matches = true;

        for (int i = 0; i < expected.length; i++) {
            matches &= checkAt(i, expected[i]);
        }

        return matches;
    }

    /**
     * Checks whether the token at the specified lookahead offset matches the expected type.
     *
     * @param offset   the offset from the current position (0 means current token)
     * @param expected the expected Token.Type for the token at the given offset
     * @return true if the token at the offset matches, false otherwise
     */
    default boolean checkAt(int offset, Token.Type... expected) {
        return List.of(expected).contains(lookAt(offset).type());
    }

    /**
     * Returns the current token without consuming it.
     * Alias for peek().
     *
     * @return the current Token object
     */
    default Token current() {
        return peek();
    }

    /**
     * Saves the current cursor position as a Savepoint, which can be used later to restore the cursor.
     *
     * @return a Savepoint representing the current position in the token stream
     */
    default Savepoint savepoint() {
        // ⚠️ The position is READ NOW and captured. It used to be `this::position` — a method reference,
        // which is a live view of wherever the cursor has since moved to, so `restore` assigned the
        // cursor its own current value and did nothing at all.
        //
        // ⚠️ It looked right and stayed invisible for as long as it did because everything that
        // backtracked only ever consumed newlines speculatively, and un-consuming a newline changes
        // nothing anybody can see. The first parser to speculatively read something that mattered — a
        // comment — lost it, on every file, with no error.
        int position = position();

        return () -> position;
    }

    /**
     * Restores the cursor to the position represented by the provided Savepoint.
     *
     * @param sp the Savepoint to restore
     */
    void restore(Savepoint sp);

    /**
     * Returns the current position (index) of the cursor within the token stream.
     *
     * @return the current token index
     */
    int position();

    /**
     * Resets the cursor to the beginning of the token stream.
     */
    void reset();

    /**
     * Returns the total number of tokens in the token stream.
     *
     * @return the total token count
     */
    int total();

    /**
     * Returns the number of tokens remaining from the current cursor position.
     *
     * @return the count of remaining tokens
     */
    int remaining();

    /**
     * Represents a marker for a position in the token stream, used for backtracking.
     */
    @FunctionalInterface
    interface Savepoint {
        /**
         * Returns the index in the token stream where this savepoint was created.
         *
         * @return the token index
         */
        int getPosition();
    }
}
