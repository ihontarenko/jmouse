package org.jmouse.gerber.excellon.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.gerber.GerberPosition;

/**
 * A node of an Excellon syntax tree: a jME expression that writes itself into jME's {@link SourceWriter}.
 * The tree is the truth: a program builds statements, never text.
 */
public abstract sealed class ExcellonNode extends AbstractExpression permits ExcellonProgram, ExcellonStatement {

    private final GerberPosition position;

    protected ExcellonNode(GerberPosition position) {
        this.position = position == null ? GerberPosition.NONE : position;
    }

    public GerberPosition position() {
        return position;
    }

    @Override
    public abstract void writeSource(SourceWriter writer);

    /** The node's text: its words written together, a statement to a line. */
    @Override
    public final String toSource() {
        return TextSourceWriter.render(this, SourceLayout.GLUED);
    }

    @Override
    public String toString() {
        return toSource();
    }
}
