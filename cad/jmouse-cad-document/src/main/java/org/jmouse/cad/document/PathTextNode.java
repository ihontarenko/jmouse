package org.jmouse.cad.document;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.svg.path.node.PathData;

/**
 * Path data a document writes in quotes — {@code "M-4 0H40"} —, held as its tree. It writes itself back from the tree, so
 * a path a program built and one an author typed are written the same way.
 */
public class PathTextNode extends AbstractExpression {

    private final PathData path;

    public PathTextNode(PathData path) {
        this.path = path;
    }

    public PathData path() {
        return path;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return path;
    }

    @Override
    public String toString() {
        return path.toSource();
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.text(path.toSource());
    }
}
