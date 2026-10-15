package org.jmouse.el.lexer;

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
     * Returns the source these tokens were read from.
     *
     * @return the tokenizable source backing this cursor
     */
    @Override
    public TokenizableSource source() {
        return source;
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
     * Moves the cursor back by one and returns the token now under it — the mirror of {@link #next()},
     * so {@code next()} followed by {@code previous()} returns the same token twice.
     *
     * <p>⚠️ It returned the token that <em>was</em> current and then stepped back
     * ({@code tokens.get(cursor--)}) — one token too late. Nothing in the reactor called it.</p>
     *
     * @return the previous {@link Token}, or {@code null} at the start
     */
    @Override
    public Token previous() {
        if (!hasPrevious()) {
            return null;
        }

        cursor--;

        return tokens.get(cursor);
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
     * <p>Clamped at both ends: a distance past the end answers the last token and a distance before the
     * start answers the first. In a lexed stream those are the {@code T_EOL} and {@code T_SOL} markers,
     * so a look outside the stream meets a boundary rather than an exception, and
     * {@link #checkAt(int, Token.Type...)} can always ask a token for its type.</p>
     *
     * <p>⚠️ Only the upper end was clamped: a negative distance near the start — {@code lookAt(-1)} at
     * position 0, which is what {@code isPrevious} asks — threw {@link IndexOutOfBoundsException}.</p>
     *
     * @param offset the offset from the current cursor position
     * @return the token at the clamped position, or {@code null} only when there are no tokens at all
     */
    @Override
    public Token lookAt(int offset) {
        if (tokens.isEmpty()) {
            return null;
        }

        return tokens.get(Math.clamp((long) cursor + offset, 0, tokens.size() - 1));
    }

    /**
     * Looks for the first token after a token of the given type
     * without moving the cursor.
     *
     * @param type the token type to look after
     * @return the token immediately following the matching token,
     *         or {@code null} if no such token exists
     */
    @Override
    public Token lookAfter(Token.Type type) {
        for (int i = cursor; i < tokens.size() - 1; i++) {
            if (tokens.get(i).type() == type) {
                return tokens.get(i + 1);
            }
        }

        return null;
    }

    /**
     * Looks for the first token before a token of the given type
     * without moving the cursor.
     *
     * @param type the token type to look before
     * @return the token immediately preceding the matching token,
     *         or {@code null} if no such token exists
     */
    @Override
    public Token lookBefore(Token.Type type) {
        for (int i = cursor; i > 0; i--) {
            if (tokens.get(i).type() == type) {
                return tokens.get(i - 1);
            }
        }

        return null;
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
     * Expects the current token — the one {@link #next()} consumes — to have one of the given types, and
     * consumes it.
     *
     * <p>⚠️ It tested the token <em>after</em> the current one ({@code isNext}) and then consumed the
     * current one, so it checked one token and took another. It now checks what it takes, which makes
     * it the same check as {@link #ensure(Token.Type...)}.</p>
     *
     * @param expected the expected token types
     * @return the consumed token
     * @throws SyntaxErrorException if the current token has none of the expected types
     */
    @Override
    public Token expect(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source, lookAt(0), expected);
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
