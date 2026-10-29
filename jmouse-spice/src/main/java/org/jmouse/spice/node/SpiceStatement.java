package org.jmouse.spice.node;

import org.jmouse.el.node.Trivia;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.spice.SpicePosition;

/** One statement of a deck: an element line, or a dot card. */
public abstract sealed class SpiceStatement extends SpiceNode permits SpiceElement, SpiceCard {

    protected SpiceStatement(SpicePosition position) {
        super(position);
    }

    /** The statement: its leading comments, its line with its trailing comment, then what it holds. */
    @Override
    public final void writeSource(SourceWriter writer) {
        getLeadingTrivia().forEach(trivia -> writeComment(writer, trivia));
        writeLine(writer);
        Trivia trailing = getTrailingTrivia();
        if (trailing != null) {
            writeTrailingComment(writer, trailing);
        }
        writer.endLine();
        writeBody(writer);
    }

    /** The words of the statement's own line. */
    protected abstract void writeLine(SourceWriter writer);

    /** The lines after it a statement holds — a subcircuit's body, a script —; none by default. */
    protected void writeBody(SourceWriter writer) {
    }

    /** A comment after a card on its line; a {@code *} comment cannot stand there, so it is written after {@code ;}. */
    private static void writeTrailingComment(SourceWriter writer, Trivia trivia) {
        String text = trivia.text();
        if (text.startsWith(";") || text.startsWith("$")) {
            writer.word(text);
        } else {
            writer.word(";").word(text.startsWith("*") ? text.substring(1).strip() : text);
        }
    }
}
