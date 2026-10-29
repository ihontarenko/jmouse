package org.jmouse.el.grammar;

import java.util.List;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

/**
 * What reads one statement of a block's body — a line by its grammar ({@link ClauseLineParser}), a formula
 * ({@link FormulaLineParser}), a block nested in it ({@link ClauseBlockParser}) —, asked by the body that holds it
 * ({@link LineSet}), never by the engine's dispatch.
 */
public interface LineReader {

    /** Whether the cursor stands on a statement this reads. */
    boolean opens(TokenCursor cursor);

    /** Reads the statement the cursor stands on; the cursor is left at its end. */
    Expression read(TokenCursor cursor, ParserContext context);

    /** What it reads, said in a refusal: a line's word, a block's. */
    String says();

    /** Whether the cursor stands on a term this reads — {@code pin:out(…)}, {@code measure(…)} —; none by default. */
    default boolean opensTerm(TokenCursor cursor) {
        return false;
    }

    /** Reads the term the cursor stands on into the same node its line would be read into. */
    default Expression readTerm(TokenCursor cursor, ParserContext context) {
        throw GrammarRefusal.here(cursor, says() + " is written as a line, not a term");
    }

    /**
     * The readers it is: itself, or — a reader that stands for whichever of several opens — those. What a describer of
     * the language walks; reading never asks it.
     */
    default List<LineReader> alternatives() {
        return List.of(this);
    }
}
