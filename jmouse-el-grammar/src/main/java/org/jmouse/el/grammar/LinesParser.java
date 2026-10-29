package org.jmouse.el.grammar;


import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

/**
 * {@code { line … }} — the body of a block of lines: the engine's statement reader walks it (blank lines, comments, the
 * closing brace), and every statement is a line of the block's one grammar, read by its {@link #lineParser()}.
 */
public abstract class LinesParser extends StatementsParser {

    /** The parser of the block's lines, as the context holds it. */
    protected abstract Class<? extends Parser> lineParser();

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw GrammarRefusal.here(cursor, "a block of lines holds lines, not a nested '{ … }'");
        }
        Expression line = (Expression) context.getParser(lineParser()).parse(cursor, context);
        if (!Lines.lineEnd().matches(cursor)) {
            throw GrammarRefusal.here(cursor, "'" + cursor.current().value() + "' is not part of this line: it has said all it says");
        }
        return line;
    }
}
