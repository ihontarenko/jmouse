package org.jmouse.el.core.lexer;

import java.util.Iterator;
import java.util.List;

/**
 * DirectAccess implementation of {@link TokenCursor} that provides navigation through a list of tokens.
 *
 * <p>Allows forward and backward traversal, token peeking, shifting, and syntax validation.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class Cursor implements TokenCursor {

    private final TokenizableSource source; // Source of tokens
    private final List<Token>       tokens; // List of tokens
    private       int               cursor; // Current position in the token list

    /**
     * Constructs a {@code Cursor} with a source and a list of tokens.
     *
     * @param source the tokenizable source
     * @param tokens the list of tokens to iterate over
     */
    public Cursor(TokenizableSource source, List<Token> tokens) {
        this.source = source;
        this.tokens = tokens;
    }

    /**
     * Checks if there are more tokens available to read.
     *
     * @return {@code true} if there are remaining tokens, otherwise {@code false}
     */
    @Override
    public boolean hasNext() {
        return tokens.size() > cursor;
    }

    /**
     * Checks if there are previous tokens available.
     *
     * @return {@code true} if there are previous tokens, otherwise {@code false}
     */
    @Override
    public boolean hasPrevious() {
        return cursor > 0;
    }

    /**
     * Returns the next token and moves the cursor forward.
     *
     * @return the next {@link Token}, or {@code null} if no tokens remain
     */
    @Override
    public Token next() {
        return hasNext() ? tokens.get(cursor++) : null;
    }

    /**
     * Returns the previous token and moves the cursor backward.
     *
     * @return the previous {@link Token}, or {@code null} if no previous tokens exist
     */
    @Override
    public Token previous() {
        return hasPrevious() ? tokens.get(cursor--) : null;
    }

    /**
     * Peeks at the current token without moving the cursor.
     *
     * @return the current token or {@code null} if no tokens remain
     */
    @Override
    public Token peek() {
        return lookAt(0);
    }

    /**
     * Looks at a token at the given offset relative to the current position.
     *
     * @param offset the offset from the current cursor position
     * @return the token at the given offset, or {@code null} if out of bounds
     */
    @Override
    public Token lookAt(int offset) {
        int position = Math.min(cursor + offset, tokens.size() - 1);
        return position < tokens.size() ? tokens.get(position) : null;
    }

    /**
     * Moves the cursor forward by a given number of positions.
     *
     * @param count the number of positions to shift forward
     */
    @Override
    public void shift(int count) {
        count = Math.abs(count); // Ensure count is positive
        cursor = Math.min(cursor + count, tokens.size());
    }

    /**
     * Moves the cursor backward by a given number of positions.
     *
     * @param count the number of positions to move backward
     */
    @Override
    public void retract(int count) {
        count = -Math.abs(count); // Ensure count is negative
        cursor = Math.max(cursor + count, 0);
    }

    /**
     * Expects a specific token type at the next position and advances if found.
     *
     * @param expected the expected token type
     * @return the next token if it matches the expected type
     * @throws SyntaxErrorException if the next token does not match the expected type
     */
    @Override
    public Token expect(Token.Type... expected) throws SyntaxErrorException {
        if (!isNext(expected)) {
            throw new SyntaxErrorException(source, lookAt(1), expected);
        }
        return next();
    }

    /**
     * Expects a specific token type at the current position and advances if found.
     *
     * @param expected the expected token type
     * @return the next token if it matches the expected type
     * @throws SyntaxErrorException if the current token does not match the expected type
     */
    @Override
    public Token ensure(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source, lookAt(0), expected);
        }

        next();

        return lookAt(-1);
    }

    /**
     * Restores the cursor position from a given savepoint.
     *
     * @param sp the savepoint to restore from
     */
    @Override
    public void restore(Savepoint sp) {
        this.cursor = sp.getPosition();
    }

    /**
     * Returns the current cursor position.
     *
     * @return the current token index
     */
    @Override
    public int position() {
        return cursor;
    }

    /**
     * Resets the cursor to the beginning of the token list.
     */
    @Override
    public void reset() {
        cursor = 0;
    }

    /**
     * Returns the total number of tokens.
     *
     * @return the total number of tokens
     */
    @Override
    public int total() {
        return tokens.size();
    }

    /**
     * Returns the number of tokens remaining after the current cursor position.
     *
     * @return the number of remaining tokens
     */
    @Override
    public int remaining() {
        return tokens.size() - cursor;
    }

    @Override
    public Iterator<Token> iterator() {
        return tokens.iterator();
    }

    @Override
    public String toString() {
        return "Cursor[%s:%d] = (%s)".formatted(current().type(), cursor, current().value());
    }
}
