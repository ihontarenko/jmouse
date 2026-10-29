package org.jmouse.strokefont.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;

/** A node of a FontoBene tree: the document, or one of its lines. Its text is written from the tree. */
public abstract sealed class FontoBeneNode extends AbstractExpression permits FontoBeneDocument, FontoBeneLine {

    private final int line;

    protected FontoBeneNode(int line) {
        this.line = line;
    }

    /** The line it was read from; 0 for one built. */
    public int line() {
        return line;
    }

    @Override
    public abstract void writeSource(SourceWriter writer);

    @Override
    public final String toSource() {
        return TextSourceWriter.render(this, SourceLayout.GLUED);
    }

    @Override
    public String toString() {
        return toSource();
    }
}
