package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

import java.util.Set;

/**
 * What a {@link ValueKind} reads a value with: the cursor, the same cursor bounded where the value ends — the grammar's
 * next word, a text in quotes, the line's end —, the parser context, the words it ends at, the words a slot allows, and
 * where the line began, for a refusal.
 */
public record ValueReading(TokenCursor cursor, TokenCursor bounded, ParserContext context, Set<String> stops,
                           Set<String> allowed, Token at, LineGrammar line) {

    /** A refusal saying the line's shape, where the line began. */
    public GrammarRefusal refusal() {
        return GrammarRefusal.at(cursor, at, line.shape());
    }

    /** The bound narrowed further: a list's item at its comma, a range's low end at its {@code ..}. */
    public TokenCursor bounded(BoundedTokenCursor.Boundary boundary) {
        return new BoundedTokenCursor(bounded, boundary);
    }

    /** An expression to where the bound says, cut out as its author wrote it. */
    public FormulaNode formula() {
        return formula(bounded);
    }

    public FormulaNode formula(TokenCursor within) {
        if (!within.hasNext()) {
            throw refusal();
        }
        Token first = cursor.current();
        Token last = first;
        while (within.hasNext()) {
            last = cursor.next();
        }
        return new FormulaNode(AuthoredText.of(cursor).written(first, last));
    }

    /** A text in quotes, or a refusal where none stands. */
    public WordNode quoted() {
        if (!cursor.isCurrent(BasicToken.T_STRING)) {
            throw refusal();
        }
        return new WordNode(QuotedText.of(cursor.next()), true);
    }
}
