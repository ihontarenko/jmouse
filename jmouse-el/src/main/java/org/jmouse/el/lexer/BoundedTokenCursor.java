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
    private int limit = -1;

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
        return cursor.position() >= start && cursor.hasNext() && !endsAt(0);
    }

    @Override
    public boolean hasPrevious() {
        return Math.min(cursor.position(), limit()) > start;
    }

    @Override
    public Token next() {
        if (!hasNext()) {
            return end(cursor.lookAt(limit() - cursor.position()));
        }
        return cursor.next();
    }

    @Override
    public Token previous() {
        if (!hasPrevious()) {
            return null;
        }
        int target = Math.min(cursor.position(), limit()) - 1;
        cursor.restore(() -> target);
        return cursor.peek();
    }

    @Override
    public Token peek() {
        return lookAt(0);
    }

    @Override
    public Token lookAt(int offset) {
        long target = (long) cursor.position() + offset;
        if (target >= limit()) {
            return end(cursor.lookAt(limit() - cursor.position()));
        }
        return cursor.lookAt((int) Math.max(start - cursor.position(), offset));
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
        for (int position = Math.min(cursor.position(), limit() - 1); position > start; position--) {
            if (cursor.lookAt(position - cursor.position()).type() == type) {
                return cursor.lookAt(position - cursor.position() - 1);
            }
        }
        return null;
    }

    @Override
    public void shift(int count) {
        int target = (int) Math.min((long) cursor.position() + Math.abs((long) count), limit());
        cursor.restore(() -> target);
    }

    @Override
    public void retract(int count) {
        int target = (int) Math.max((long) cursor.position() - Math.abs((long) count), start);
        cursor.restore(() -> target);
    }

    @Override
    public Token expect(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source(), peek(), expected);
        }
        return next();
    }

    @Override
    public Token ensure(Token.Type... expected) throws SyntaxErrorException {
        if (!isCurrent(expected)) {
            throw new SyntaxErrorException(source(), peek(), expected);
        }
        return next();
    }

    @Override
    public void restore(Savepoint savepoint) {
        if (savepoint.getPosition() < start || savepoint.getPosition() > limit()) {
            throw new IllegalArgumentException("Savepoint is outside the bounded cursor");
        }
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
        return Math.max(0, limit() - cursor.position());
    }

    @Override
    public Iterator<Token> iterator() {
        List<Token> visible = new ArrayList<>();
        int count = remaining();
        for (int ahead = 0; ahead < count; ahead++) {
            visible.add(cursor.lookAt(ahead));
        }
        return visible.iterator();
    }

    /** Whether the view ends at the token {@code ahead} of the cursor. */
    private boolean endsAt(int ahead) {
        return (long) cursor.position() + ahead >= limit();
    }

    /**
     * Boundaries are pure predicates over an immutable token stream. Compute the first boundary
     * once, even when another view moves the shared cursor or a savepoint is restored.
     */
    private int limit() {
        if (limit >= 0) {
            return limit;
        }
        int depth = 0;
        for (int position = start; position < cursor.total(); position++) {
            int offset = position - cursor.position();
            Token token = cursor.lookAt(offset);
            if (boundary.endsAt(token, cursor.lookAt(offset + 1), depth)) {
                limit = position;
                return limit;
            }
            Token.Type type = token.type();
            if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            }
        }
        limit = cursor.total();
        return limit;
    }

    /** The end of the view, where a token stands: what a parser reads as the end of a text. */
    private static Token end(Token at) {
        if (at == null) {
            return new Token("", BasicToken.T_EOL, 0, 0, 1);
        }
        return new Token("", BasicToken.T_EOL, 0, at.offset(), at.lineNumber());
    }
}
