package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.writer.SourceWriter;

/** {@code 24 x 12}: how wide and how high an element is, around its centre. */
public class ExtentNode extends AbstractExpression {

    private final QuantityLiteralNode width;
    private final QuantityLiteralNode height;

    public ExtentNode(QuantityLiteralNode width, QuantityLiteralNode height) {
        this.width = width;
        this.height = height;
    }

    public double width() {
        return width.getQuantity().value();
    }

    public double height() {
        return height.getQuantity().value();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return new double[] {width(), height()};
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.write(width).word("x").write(height);
    }
}
