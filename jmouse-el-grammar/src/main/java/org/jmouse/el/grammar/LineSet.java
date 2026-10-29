package org.jmouse.el.grammar;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The statements a body may hold, in the order they are asked: the first reader that opens a statement reads it. A line
 * with a word of its own comes before a line that opens with a name, which would open on any word.
 */
public record LineSet(List<LineReader> readers) {

    public LineSet {
        readers = List.copyOf(readers);
    }

    public static LineSet of(LineReader... readers) {
        return new LineSet(List.of(readers));
    }

    /** The lines of these grammars, word-led ones first. */
    public static LineSet of(List<LineGrammar> grammars) {
        return new LineSet(grammars.stream().sorted((one, other) -> Boolean.compare(one.isNamed(), other.isNamed()))
                .map(ClauseLineParser::new).map(LineReader.class::cast).toList());
    }

    /**
     * Reads the statement the cursor stands on, written either way: as a term — its head touching a {@code :} or a
     * {@code (} — or as a line. Nothing says which: a line never begins as a term does, so each statement tells.
     */
    public Expression read(TokenCursor cursor, ParserContext context) {
        for (LineReader reader : readers) {
            if (reader.opensTerm(cursor)) {
                return reader.readTerm(cursor, context);
            }
        }
        return readers.stream().filter(reader -> reader.opens(cursor)).findFirst()
                .orElseThrow(() -> GrammarRefusal.here(cursor, "'" + cursor.current().value() + "' is not a line here: "
                        + readers.stream().map(LineReader::says).collect(Collectors.joining(", "))))
                .read(cursor, context);
    }
}
