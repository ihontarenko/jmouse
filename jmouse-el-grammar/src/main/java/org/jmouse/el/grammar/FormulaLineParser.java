package org.jmouse.el.grammar;


import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads {@code left = right} to the end of its line: the one {@code =} outside parentheses divides it, and each side is
 * cut out as written for jMF — {@code ==} is a comparison inside a side, never the divide.
 */
public class FormulaLineParser implements LineReader {

    private static final String ASSIGNMENT = "=";

    /** The left side ends at the one {@code =} outside parentheses. */
    private static final BoundedTokenCursor.Boundary DIVIDE = (token, next, depth) -> Lines.LINE.endsAt(token, next, depth)
            || depth == 0 && token.type() == BasicToken.T_EQ && token.value().equals(ASSIGNMENT);

    private final String shape;

    /** @param shape the line as an author writes it, said in a refusal */
    public FormulaLineParser(String shape) {
        this.shape = shape;
    }

    @Override
    public boolean opens(TokenCursor cursor) {
        return !Lines.lineEnd().matches(cursor) && !cursor.isCurrent(BasicToken.T_STRING);
    }

    @Override
    public String says() {
        return "a formula 'left = right'";
    }

    @Override
    public FormulaLineNode read(TokenCursor cursor, ParserContext context) {
        Token first = cursor.current();
        FormulaNode left = side(new BoundedTokenCursor(cursor, DIVIDE), cursor, first);
        if (!cursor.current().value().equals(ASSIGNMENT)) {
            throw GrammarRefusal.at(cursor, first, shape);
        }
        cursor.next();
        FormulaNode right = side(LineBoundaries.line(cursor), cursor, first);
        return new FormulaLineNode(left, right, new FormulaNode(AuthoredText.of(cursor).written(first, cursor.lookAt(-1))));
    }

    private FormulaNode side(TokenCursor bounded, TokenCursor cursor, Token at) {
        if (!bounded.hasNext()) {
            throw GrammarRefusal.at(cursor, at, shape);
        }
        Token first = cursor.current();
        Token last = first;
        while (bounded.hasNext()) {
            last = cursor.next();
        }
        return new FormulaNode(AuthoredText.of(cursor).written(first, last));
    }
}
