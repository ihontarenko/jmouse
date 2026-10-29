package org.jmouse.el.grammar;

import java.util.List;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.writer.SourceWriter;

/** {@code x, y}: a point of a drawing, each coordinate a number with at most its unit. */
public class PointNode extends AbstractExpression {

    private final QuantityLiteralNode x;
    private final QuantityLiteralNode y;

    public PointNode(QuantityLiteralNode x, QuantityLiteralNode y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x.getQuantity().value();
    }

    public double y() {
        return y.getQuantity().value();
    }

    /** The coordinates as written: {@code -30}, {@code 0}. */
    public List<QuantityLiteralNode> coordinates() {
        return List.of(x, y);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return new double[] {x(), y()};
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.write(x).join(",").write(y);
    }
}
