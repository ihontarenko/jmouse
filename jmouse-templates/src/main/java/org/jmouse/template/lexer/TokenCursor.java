package org.jmouse.template.lexer;

import java.util.List;

/**
 * TokenCursor provides an advanced API for navigating a stream of tokens.
 * It offers methods for lookahead, shifting, retracting, expectation checking,
 * and managing savepoints for backtracking.
 *
 * <p>This interface is intended to be used by parsers to inspect and consume tokens
 * with support for backtracking and lookahead. It is built on top of an underlying token list.</p>
 */
public interface TokenCursor {

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
     * @return the next token
     */
    Token next();

    /**
     * Consumes and returns the previous token in the stream.
     *
     * @return the previous token
     */
    Token previous();

    /**
     * Returns the next token without consuming it.
     *
     * @return the next token
     */
    Token peek();

    /**
     * Returns the token at the specified offset from the current position without consuming any tokens.
     * For example, lookAt(0) is equivalent to peek(), lookAt(1) returns the token immediately after peek(), etc.
     *
     * @param offset the offset from the current position (0-based)
     * @return the token at the given offset
     */
    Token lookAt(int offset);

    /**
     * Returns a list of tokens starting from the current position, up to the specified count.
     *
     * @param count the number of tokens to retrieve
     * @return a list of tokens from the current position
     */
    List<Token> lookOver(int count);

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
     * Consumes the next token and verifies that it matches the expected token.
     * If the token does not match, a SyntaxErrorException is thrown.
     *
     * @param expected the expected token
     * @return the consumed token
     * @throws SyntaxErrorException if the next token does not match the expected token
     */
    Token expect(Token expected) throws SyntaxErrorException;

    /**
     * Saves the current cursor position as a Savepoint, which can be used later to restore the cursor.
     *
     * @return a Savepoint representing the current position in the token stream
     */
    default Savepoint savepoint() {
        return this::position;
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
    int totalTokens();

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
