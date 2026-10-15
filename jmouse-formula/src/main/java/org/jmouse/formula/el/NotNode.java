package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

/** A leading {@code !}: the condition does not hold. Parsed by jMF's grammar, so it has a node of its own. */
public class NotNode extends UnaryOperation {

    public NotNode(Expression operand) {
        super(operand, LogicalOperator.NOT);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object value = operand.evaluate(context);
        boolean holds = value instanceof Number number ? number.doubleValue() != 0 : Boolean.TRUE.equals(value);
        return holds ? 0 : 1;
    }

    @Override
    public String toSource() {
        return "!(" + operand.toSource() + ")";
    }

    @Override
    public String toString() {
        return "( NOT : " + operand + " )";
    }
}
