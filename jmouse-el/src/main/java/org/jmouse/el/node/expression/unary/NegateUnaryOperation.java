package org.jmouse.el.node.expression.unary;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

public class NegateUnaryOperation extends UnaryOperation {

    public NegateUnaryOperation(Expression operand) {
        super(operand, LogicalOperator.NOT);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return operator.getCalculator().calculate(operand.evaluate(context));
    }

    @Override
    public String toString() {
        return "( %s : %s )".formatted(operator.getName(), operand);
    }

}
