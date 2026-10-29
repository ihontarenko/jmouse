package org.jmouse.el.grammar;


import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

import java.util.List;

/**
 * The lines of a drawn kind's body, each read by the grammar of its word: a module subclasses it once per kind with its
 * grammars ({@link #grammars()}), registers the subclass, and names it as the kind's body lines
 * (a document kind's lines, say). A new line is one more grammar, never another reader.
 */
public abstract class GrammarLinesParser extends AbstractParser {

    private LineSet lines;

    /** The grammars of the body's lines. */
    protected abstract List<LineGrammar> grammars();

    /** What the body holds: its lines by their grammars, and the blocks a body of its kind may hold among them. */
    /** The lines it reads. */
    public LineSet lineSet() {
        return LineSet.of(grammars());
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (lines == null) {
            lines = lineSet();
        }
        parent.add(lines.read(cursor, context));
        if (!Lines.lineEnd().matches(cursor)) {
            throw GrammarRefusal.here(cursor, "'" + cursor.current().value() + "' is not part of this line");
        }
    }
}
