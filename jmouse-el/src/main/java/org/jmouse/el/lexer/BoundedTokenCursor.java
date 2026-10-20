package org.jmouse.el.lexer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A view of a cursor that ends where a {@link Boundary} says — the stream goes on, but not for whoever reads through
 * the view. A parser handed the view stops there as it stops at the end of a text, and since the view moves the cursor
 * it wraps, the cursor is left on the token the view ended at.
 *
 * <p>What it is for: a language that writes its own words after an expression. In jMX's
 * {@code t = 0 in 0 .. 2 pi step 0.01}, only the line's grammar knows that {@code step} is its word and {@code pi} a
 * factor; it reads the bound through a view that ends at {@code step}, and the expression parser — juxtaposition and
 * all — never sees it. The grammar says where; the parser is not told anything.</p>
 */
public final class BoundedTokenCursor implements TokenCursor {

    /** Where a view ends. */
    @FunctionalInterface
    public interface Boundary {

        /**
         * @param token the token the cursor stands on
         * @param next  the token after it
         * @param depth how many parentheses and brackets the view has opened and not closed before the token
         * @return {@code true} when the view ends before this token
         */
        boolean endsAt(Token token, Token next, int depth);
    }

    private final TokenCursor cursor;
    private final Boundary boundary;
    private final int start;

    public BoundedTokenCursor(TokenCursor cursor, Boundary boundary) {
        this.cursor = cursor;
        this.boundary = boundary;
        this.start = cursor.position();
    }

    @Override
    public TokenizableSource source() {
        return cursor.source();
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext() && !endsAt(0);
    }

    @Override
    public boolean hasPrevious() {
        return cursor.position() > start;
    }

    @Override
    public Token next() {
        if (endsAt(0)) {
            return end(cursor.lookAt(0));
        }
        return cursor.next();
    }

    @Override
    public Token previous() {
        return cursor.previous();
    }

    @Override
    public Token peek() {
        return lookAt(0);
    }

    @Override
    public Token lookAt(int offset) {
        for (int ahead = 0; ahead <= offset; ahead++) {
            if (endsAt(ahead)) {
                return end(cursor.lookAt(ahead));
            }
        }
        return cursor.lookAt(offset);
    }

    @Override
    public Token lookAfter(Token.Type type) {
        for (int ahead = 0; cursor.position() + ahead < cursor.total() && !endsAt(ahead); ahead++) {
            if (cursor.lookAt(ahead).type() == type) {
                return endsAt(ahead + 1) ? null : cursor.lookAt(ahead + 1);
            }
        }
        return null;
    }

    @Override
    public Token lookBefore(Token.Type type) {
        return cursor.lookBefore(type);
    }

    @Override
    public void shift(int count) {
        for (int shifted = 0; shifted < count && hasNext(); shifted++) {
            cursor.next();
        }
    }

    @Override
    public void retract(int count) {
        cursor.retract(count);
    }

    @Override
    public Token expect(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source(), peek(), expected);
        }
        return cursor.expect(expected);
    }

    @Override
    public Token ensure(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source(), peek(), expected);
        }
        return cursor.ensure(expected);
    }

    @Override
    public void restore(Savepoint savepoint) {
        cursor.restore(savepoint);
    }

    @Override
    public int position() {
        return cursor.position();
    }

    @Override
    public void reset() {
        cursor.restore(() -> start);
    }

    @Override
    public int total() {
        return cursor.total();
    }

    @Override
    public int remaining() {
        int remaining = 0;
        while (cursor.position() + remaining < cursor.total() && !endsAt(remaining)) {
            remaining++;
        }
        return remaining;
    }

    @Override
    public Iterator<Token> iterator() {
        List<Token> visible = new ArrayList<>();
        for (int ahead = 0; ahead < remaining(); ahead++) {
            visible.add(cursor.lookAt(ahead));
        }
        return visible.iterator();
    }

    /** Whether the view ends at the token {@code ahead} of the cursor. */
    private boolean endsAt(int ahead) {
        int depth = 0;
        for (int at = start - cursor.position(); at < ahead; at++) {
            Token.Type type = cursor.lookAt(at).type();
            if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            }
        }
        return boundary.endsAt(cursor.lookAt(ahead), cursor.lookAt(ahead + 1), depth);
    }

    /** The end of the view, where a token stands: what a parser reads as the end of a text. */
    private static Token end(Token at) {
        return new Token("", BasicToken.T_EOL, 0, at.offset(), at.lineNumber());
    }
}
