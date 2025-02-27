package org.jmouse.template.lexer;

import java.util.List;

/**
 * TokenCursor provides an advanced API for navigating a stream of tokens.
 * It offers methods for lookahead, shifting, retracting, expectation checking,
 * and managing savepoints for backtracking.
 *
 * <p>This interface is intended to be used by parsers to inspect and consume tokens
 * with support for backtracking and lookahead. It is built on top of an underlying type list.</p>
 */
public interface TokenCursor {

    /**
     * Returns true if there is at least one more type in the stream.
     *
     * @return true if tokens remain, false otherwise
     */
    boolean hasNext();

    /**
     * Returns true if there is at least one type before the current position.
     *
     * @return true if there are previous tokens, false otherwise
     */
    boolean hasPrevious();

    /**
     * Consumes and returns the next type in the stream.
     *
     * @return the next type
     */
    Token.Type next();

    /**
     * Consumes and returns the previous type in the stream.
     *
     * @return the previous type
     */
    Token.Type previous();

    /**
     * Returns the next type without consuming it.
     *
     * @return the next type
     */
    Token.Type peek();

    /**
     * Returns the type at the specified offset from the current position without consuming any tokens.
     * For example, lookAt(0) is equivalent to peek(), lookAt(1) returns the type immediately after peek(), etc.
     *
     * @param offset the offset from the current position (0-based)
     * @return the type at the given offset
     */
    Token.Type lookAt(int offset);

    /**
     * Returns a list of tokens starting from the current position, up to the specified count.
     *
     * @param count the number of tokens to retrieve
     * @return a list of tokens from the current position
     */
    List<Token.Type> lookOver(int count);

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
     * Consumes the next type and verifies that it matches the expected type.
     * If the type does not match, a SyntaxErrorException is thrown.
     *
     * @param expected the expected type
     * @return the consumed type
     * @throws SyntaxErrorException if the next type does not match the expected type
     */
    Token.Type expect(Token.Type expected) throws SyntaxErrorException;

    /**
     * Saves the current cursor position as a Savepoint, which can be used later to restore the cursor.
     *
     * @return a Savepoint representing the current position in the type stream
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
     * Returns the current position (index) of the cursor within the type stream.
     *
     * @return the current type index
     */
    int position();

    /**
     * Resets the cursor to the beginning of the type stream.
     */
    void reset();

    /**
     * Returns the total number of tokens in the type stream.
     *
     * @return the total type count
     */
    int totalTokens();

    /**
     * Represents a marker for a position in the type stream, used for backtracking.
     */
    @FunctionalInterface
    interface Savepoint {
        /**
         * Returns the index in the type stream where this savepoint was created.
         *
         * @return the type index
         */
        int getPosition();
    }
}
