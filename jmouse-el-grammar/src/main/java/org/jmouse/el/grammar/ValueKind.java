package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;

import java.util.Set;

/**
 * What a value of a line is, and how it is read: a word, a quantity, a point, an expression, a list… The library's own
 * kinds are {@link Values}; a language adds its own by implementing this — SVG path data in quotes, a pin's number —, and
 * a grammar names them like any other. A new kind is a new implementation; the grammar never changes for it.
 */
public interface ValueKind {

    /**
     * Whether a value of this kind stands where the cursor is, for a slot that may be left out: by default anything that
     * is no text in quotes and no word of the grammar, and one of the {@code allowed} words when the slot names them.
     * The line has not ended where this is asked.
     *
     * @param words the grammar's words, where a value never begins
     */
    default boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
        Token current = cursor.current();
        return current.type() != BasicToken.T_STRING && !words.contains(current.value())
                && (allowed.isEmpty() || allowed.contains(Lines.peekWord(cursor)));
    }

    /** Reads the value where the cursor stands; the cursor is left after it. Null only for a value that is its word. */
    Expression read(ValueReading reading);

    /**
     * Reads the value as a term's argument writes it, to the argument's comma or closing parenthesis: by default as a line
     * writes it. A kind whose line spelling holds a comma — a point, a list — says how a term spells it.
     */
    default Expression readTerm(ValueReading reading) {
        return read(reading);
    }

    /** Whether a value of this kind stands where a term's positional argument begins; by default as in a line. */
    default boolean standsAtTerm(TokenCursor cursor, Set<String> words, Set<String> allowed) {
        return standsAt(cursor, words, allowed);
    }
}
