package org.jmouse.gerber.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.gerber.GerberPosition;

/**
 * A node of a Gerber syntax tree: a jME expression, so a file is walked and its children reached the way any jME tree
 * is, and every node writes itself into jME's {@link SourceWriter} — {@link #toSource()} is that, laid out. The tree is
 * the truth: a program builds nodes, never text, and the file is the root's source.
 */
public abstract sealed class GerberNode extends AbstractExpression permits GerberDocument, GerberCommand {

    private final GerberPosition position;

    protected GerberNode(GerberPosition position) {
        this.position = position == null ? GerberPosition.NONE : position;
    }

    /** Where the node begins in the text it was read from; {@link GerberPosition#NONE} for one a program built. */
    public GerberPosition position() {
        return position;
    }

    @Override
    public abstract void writeSource(SourceWriter writer);

    /** The node's text: its words, written together as a word-address format writes them, a block to a line. */
    @Override
    public final String toSource() {
        return TextSourceWriter.render(this, SourceLayout.GLUED);
    }

    @Override
    public String toString() {
        return toSource();
    }
}
