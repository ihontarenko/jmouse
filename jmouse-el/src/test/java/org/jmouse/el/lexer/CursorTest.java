package org.jmouse.el.lexer;

import org.jmouse.el.StringSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cursor every jME parser moves through: which token is current, what each move returns, and what a
 * look outside the stream answers.
 */
class CursorTest {

    private final Token start      = token("", BasicToken.T_SOL, 0);
    private final Token identifier = token("a", BasicToken.T_IDENTIFIER, 1);
    private final Token plus       = token("+", BasicToken.T_PLUS, 2);
    private final Token number     = token("1", BasicToken.T_INT, 3);
    private final Token end        = token("", BasicToken.T_EOL, 4);

    private Cursor cursor() {
        return new Cursor(new StringSource("cursor-test", "a+1"), List.of(start, identifier, plus, number, end));
    }

    private static Token token(String value, Token.Type type, int ordinal) {
        return new Token(value, type, ordinal, ordinal, 1);
    }

    @Test
    @DisplayName("next returns the current token and moves on; previous moves back and returns the token now current")
    void previousMirrorsNext() {
        Cursor cursor = cursor();

        assertSame(start, cursor.next());
        assertSame(identifier, cursor.next());
        assertSame(plus, cursor.peek());

        assertSame(identifier, cursor.previous());
        assertEquals(1, cursor.position());
        assertSame(identifier, cursor.peek());
        assertSame(start, cursor.previous());
        assertNull(cursor.previous(), "Nothing before the first token.");
        assertEquals(0, cursor.position());
    }

    @Test
    @DisplayName("lookAt is clamped at both ends and never moves the cursor")
    void lookAtIsClampedAtBothEnds() {
        Cursor cursor = cursor();

        assertSame(start, cursor.lookAt(-1), "Before the start is the first token, not an exception.");
        assertSame(start, cursor.lookAt(-100));
        assertFalse(cursor.isPrevious(BasicToken.T_IDENTIFIER));
        assertSame(end, cursor.lookAt(100));

        cursor.shift(2);
        assertSame(identifier, cursor.lookAt(-1));
        assertTrue(cursor.isPrevious(BasicToken.T_IDENTIFIER));
        assertSame(number, cursor.lookAt(1));
        assertEquals(2, cursor.position());

        assertNull(new Cursor(null, List.of()).lookAt(-1), "An empty stream has nothing to answer with.");
    }

    @Test
    @DisplayName("expect checks the token it consumes — the current one — and reports that token when it differs")
    void expectChecksTheCurrentToken() {
        Cursor cursor = cursor();
        cursor.shift(1);

        assertSame(identifier, cursor.expect(BasicToken.T_IDENTIFIER));
        assertSame(plus, cursor.peek());

        var refusal = assertThrows(SyntaxErrorException.class, () -> cursor.expect(BasicToken.T_INT));
        assertTrue(refusal.getMessage().contains("T_PLUS"), refusal.getMessage());
        assertSame(plus, cursor.peek(), "A refused expectation consumes nothing.");

        assertSame(plus, cursor.expect(BasicToken.T_MINUS, BasicToken.T_PLUS));
        assertSame(number, cursor.ensure(BasicToken.T_INT));
        assertSame(end, cursor.expect(BasicToken.T_EOL));
        assertFalse(cursor.hasNext());
    }

    @Test
    @DisplayName("a savepoint restores the position it was taken at")
    void savepointRestoresThePosition() {
        Cursor cursor = cursor();
        cursor.shift(1);
        var savepoint = cursor.savepoint();

        cursor.shift(3);
        cursor.restore(savepoint);

        assertSame(identifier, cursor.peek());
    }
}
