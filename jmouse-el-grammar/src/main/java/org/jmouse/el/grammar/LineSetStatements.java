package org.jmouse.el.grammar;


import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

/**
 * {@code { statement … }} — a block's body whose statements a {@link LineSet} reads: the engine's statement reader walks
 * it (blank lines, comments, the closing brace) and asks the set for each statement. An instance per block, never
 * registered: the context holds one parser per class, and every block has a set of its own.
 */
public class LineSetStatements extends StatementsParser {

    private final LineSet lines;

    public LineSetStatements(LineSet lines) {
        this.lines = lines;
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw GrammarRefusal.here(cursor, "a block holds its statements, not a bare '{ … }'");
        }
        Expression statement = lines.read(cursor, context);
        if (!Lines.lineEnd().matches(cursor)) {
            throw GrammarRefusal.here(cursor, "'" + cursor.current().value() + "' is not part of this line: it has said all it says");
        }
        return statement;
    }
}
