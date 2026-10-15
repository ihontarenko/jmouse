package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

/** A trailing {@code !}: {@code n!} is the factorial of n, {@code (n + 1)!} of n + 1. */
public class FactorialNode extends UnaryOperation {

    public FactorialNode(Expression operand) {
        super(operand, FormulaOperator.FACTORIAL);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return operator.getCalculator().calculate(operand.evaluate(context));
    }

    @Override
    public String toSource() {
        return "(" + operand.toSource() + ")!";
    }

    @Override
    public String toString() {
        return "( FACTORIAL : " + operand + " )";
    }
}
