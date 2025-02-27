package org.jmouse.template.lexer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provides an abstract implementation of a lexer with token navigation and filtering capabilities.
 *
 * <p>This lexer supports bidirectional traversal and lookup operations over a list of tokens.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractLexer extends ImmutableListIterator<Token.Entry> implements Lexer {

    public AbstractLexer(List<Token.Entry> entries) {
        super(entries);
    }

    /**
     * Moves forward while the given predicate condition is not met.
     *
     * @param predicate the condition to stop at
     */
    @Override
    public void forward(Predicate<Token.Entry> predicate) {
        while (hasNext() && !predicate.test(current())) {
            next();
        }
    }

    /**
     * Moves forward until the specified token is found.
     *
     * @param token the token to move forward to
     */
    @Override
    public void forward(Token token) {
        forward(e -> e.is(token));
    }

    /**
     * Moves forward until the specified entry is found.
     *
     * @param entry the entry to move forward to
     */
    @Override
    public void forward(Token.Entry entry) {
        forward(e -> e.is(entry));
    }

    /**
     * Moves backward while the given predicate condition is not met.
     *
     * @param predicate the condition to stop at
     */
    @Override
    public void backward(Predicate<Token.Entry> predicate) {
        while (hasPrevious() && !predicate.test(current())) {
            previous();
        }
    }

    /**
     * Moves backward until the specified token is found.
     *
     * @param token the token to move backward to
     */
    @Override
    public void backward(Token token) {
        backward(e -> e.is(token));
    }

    /**
     * Moves backward until the specified entry is found.
     *
     * @param entry the entry to move backward to
     */
    @Override
    public void backward(Token.Entry entry) {
        backward(e -> e.is(entry));
    }

    /**
     * Returns the current token entry.
     *
     * @return the current token entry
     */
    @Override
    public Token.Entry current() {
        return entries.get(cursor);
    }

    /**
     * Returns the total length of tokens.
     *
     * @return the total token count
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * Returns the current cursor offset.
     *
     * @return the cursor offset
     */
    @Override
    public int cursor() {
        return cursor;
    }

    /**
     * Sets the cursor to the specified offset.
     *
     * @param cursor the new cursor offset
     */
    @Override
    public void cursor(int cursor) {
        this.cursor = cursor;
    }

    /**
     * Checks if the next sequence of tokens matches the given tokens.
     *
     * @param limit  the number of tokens to check
     * @param offset the offset offset
     * @param tokens the expected token sequence
     * @return {@code true} if the sequence matches, otherwise {@code false}
     */
    @Override
    public boolean is(int limit, int offset, Token... tokens) {
        if (cursor + offset < 0) {
            offset = -cursor;
        }

        Lexer       lexer    = lexer(offset);
        List<Token> expected = Arrays.asList(tokens);

        while (limit-- > 0 && lexer.hasNext()) {
            if (expected.contains(lexer.next().token())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds an entry enclosed between the specified start and end tokens.
     *
     * @param start the start token
     * @param end   the end token
     * @return the matching entry
     * @throws LexerException if the end token is not found
     */
    @Override
    public Token.Entry lookOver(Token start, Token end) {
        Lexer       lexer  = lexer();
        Token.Entry result = null;
        int         depth  = 0;
        Token.Entry next   = lexer.next();

        while (lexer.hasNext()) {
            if (next.is(start)) {
                depth++;
            }

            if (next.is(end)) {
                depth--;
            }

            if (depth == 0) {
                result = next;
                break;
            }

            next = lexer.next();
        }

        if (result == null) {
            throw new LexerException("CANNOT FIND END FOR: %s".formatted(end));
        }

        return result;
    }

    /**
     * Creates a new lexer instance at the given offset.
     *
     * @param offset the offset offset
     * @return a new lexer instance
     */
    @Override
    public Lexer lexer(int offset) {
        int   cursor = AbstractLexer.this.cursor;
        Lexer lexer  = new AbstractLexer(AbstractLexer.this.entries) {
        };

        lexer.cursor(cursor + offset);

        return lexer;
    }
}
