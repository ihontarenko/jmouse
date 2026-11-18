package org.jmouse.el.lexer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BoundedCursorContractTest {
    @Test
    void boundaryEndCanBeExpectedWithoutConsumingTheOuterToken() {
        Cursor cursor = new Cursor(null, List.of(token("outer", 0), token("body", 1), token("stop", 2)));
        cursor.next();
        BoundedTokenCursor view = new BoundedTokenCursor(cursor,
                (current, next, depth) -> current.value().equals("stop"));
        assertNull(view.previous());
        assertEquals("body", view.lookAt(-100).value());
        assertEquals("body", view.next().value());
        assertEquals(BasicToken.T_EOL, view.ensure(BasicToken.T_EOL).type());
        assertEquals(BasicToken.T_EOL, view.expect(BasicToken.T_EOL).type());
        assertEquals("stop", cursor.peek().value());
        view.retract(100);
        assertEquals(1, cursor.position());
        assertThrows(IllegalArgumentException.class, () -> view.restore(() -> 0));
    }

    @Test
    void sharedMovementAndRestoreDoNotRescanThePrefixForEachToken() {
        List<Token> tokens = new ArrayList<>();
        for (int index = 0; index < 1_000; index++) {
            tokens.add(token("value", index));
        }
        tokens.add(token("stop", 1_000));
        AtomicInteger boundaryCalls = new AtomicInteger();
        Cursor cursor = new Cursor(null, tokens);
        BoundedTokenCursor view = new BoundedTokenCursor(cursor, (current, next, depth) -> {
            boundaryCalls.incrementAndGet();
            return current.value().equals("stop");
        });
        while (view.hasNext()) {
            cursor.next();
        }
        view.reset();
        assertEquals(1_000, view.remaining());
        assertEquals(1_000, java.util.stream.StreamSupport.stream(view.spliterator(), false).count());
        assertEquals(1_001, boundaryCalls.get());
    }

    @Test
    void eofAndMissingSourceStillProduceNavigationAndSyntaxResults() {
        Cursor cursor = new Cursor(null, List.of(token("first", 0), token("second", 1)));
        cursor.next();
        cursor.next();
        assertEquals("first", cursor.lookBefore(BasicToken.T_IDENTIFIER).value());
        assertThrows(SyntaxErrorException.class, () -> cursor.expect(BasicToken.T_NUMERIC));
    }

    private static Token token(String value, int ordinal) {
        return new Token(value, BasicToken.T_IDENTIFIER, ordinal, ordinal, 1);
    }
}
