package org.jmouse.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.TokenCursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a cursor answers about the text around it, ahead and behind, without being moved. */
class TokenCursorTest {

    @Test
    void matchesWordsByText() {
        TokenCursor cursor = cursor("x = 1 step 2");
        assertEquals("=", cursor.valueAt(1));
        assertTrue(cursor.checkValueAt(1, "="));
        assertTrue(cursor.checkValueAt(3, "step", "log"));
        assertFalse(cursor.checkValueAt(0, "step"));
        assertEquals("", cursor("\"a\"").valueAt(5), "past the end there is no word");
        assertFalse(cursor("\"step\"").checkValueAt(0, "\"step\""), "a text in quotes is not a word");
    }

    @Test
    void knowsWhereALineAndWhatWasReadEnd() {
        TokenCursor cursor = cursor("a + b\nc");
        cursor.next();
        assertEquals(2, cursor.endOfRead());
        cursor.next();
        cursor.next();
        assertTrue(cursor.atLineBreak());
        TokenCursor view = new BoundedTokenCursor(cursor, (token, next, depth) -> token.type() == BasicToken.T_NEW_LINE);
        assertEquals(0, view.remaining());
        cursor.next();
        cursor.next();
        assertEquals(7, cursor.endOfRead(), "at the end, what was read ends at the end of the text");
    }

    private static TokenCursor cursor(String text) {
        TokenCursor cursor = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()))
                .tokenize(new StringSource("cursor", text));
        cursor.consumeIf(BasicToken.T_SOL);
        return cursor;
    }
}
