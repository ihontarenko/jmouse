package org.jmouse.el.node.expression.literal;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.units.Quantity;

/**
 * A value as a datasheet writes it — {@code 100 nF}, {@code 200 ppm/K}, {@code -40 °C}, {@code 25} — read whole. It
 * evaluates to its value in SI; the quantity keeps its unit and how it was written.
 */
public class QuantityLiteralNode extends LiteralNode<Double> {

    private final Quantity quantity;

    public QuantityLiteralNode(Quantity quantity) {
        super(quantity.value());
        this.quantity = quantity;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return quantity.value();
    }

    @Override
    public String toString() {
        return quantity.written();
    }
}
